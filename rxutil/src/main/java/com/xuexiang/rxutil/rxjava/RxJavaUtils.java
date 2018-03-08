/*
 * Copyright (C) 2018 xuexiangjys(xuexiangjys@163.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xuexiang.rxutil.rxjava;

import android.support.annotation.NonNull;

import com.xuexiang.rxutil.SimpleThrowableAction;
import com.xuexiang.rxutil.rxjava.task.CommonRxTask;
import com.xuexiang.rxutil.rxjava.task.RxIOTask;
import com.xuexiang.rxutil.rxjava.task.RxUITask;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * RxJava工具
 *
 * @author xuexiang
 * @date 2018/3/4 上午1:20
 */
public final class RxJavaUtils {

    private final static String TAG = "RxJavaUtils";

    //========================线程任务==========================//
    /**
     * 在ui线程中工作
     *
     * @param uiTask 在UI线程中操作的任务
     * @param <T>
     * @return
     */
    public static <T> Subscription doInUIThread(RxUITask<T> uiTask) {
        return doInUIThread(uiTask, new SimpleThrowableAction(TAG));
    }

    /**
     * 在ui线程中工作
     *
     * @param uiTask      在UI线程中操作的任务
     * @param errorAction 出错的处理
     * @param <T>
     * @return
     */
    public static <T> Subscription doInUIThread(RxUITask<T> uiTask, Action1<Throwable> errorAction) {
        return Observable.just(uiTask)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<RxUITask<T>>() {
                    @Override
                    public void call(RxUITask rxUITask) {
                        rxUITask.doInUIThread(rxUITask.getInData());
                    }
                }, errorAction);
    }

    /**
     * 在IO线程中执行任务
     *
     * @param ioTask 在io线程中操作的任务
     * @param <T>
     * @return
     */
    public static <T> Subscription doInIOThread(RxIOTask<T> ioTask) {
        return doInIOThread(ioTask, new SimpleThrowableAction(TAG));
    }

    /**
     * 在IO线程中执行任务
     *
     * @param ioTask      在io线程中操作的任务
     * @param errorAction 出错的处理
     * @param <T>
     * @return
     */
    public static <T> Subscription doInIOThread(RxIOTask<T> ioTask, Action1<Throwable> errorAction) {
        return Observable.just(ioTask)
                .observeOn(Schedulers.io())
                .subscribe(new Action1<RxIOTask<T>>() {
                    @Override
                    public void call(RxIOTask<T> rxIOTask) {
                        rxIOTask.doInIOThread(rxIOTask.getInData());
                    }
                }, errorAction);
    }

    /**
     * 执行Rx通用任务 (IO线程中执行耗时操作 执行完成调用UI线程中的方法)
     *
     * @param rxTask 执行任务
     * @param <T>
     * @return
     */
    public static <T, R> Subscription executeRxTask(CommonRxTask<T, R> rxTask) {
        return executeRxTask(rxTask, new SimpleThrowableAction(TAG));
    }

    /**
     * 执行Rx通用任务 (IO线程中执行耗时操作 执行完成调用UI线程中的方法)
     *
     * @param rxTask      执行任务
     * @param errorAction 出错的处理
     * @param <T>
     * @return
     */
    public static <T, R> Subscription executeRxTask(CommonRxTask<T, R> rxTask, Action1<Throwable> errorAction) {
        RxTaskOnSubscribe<CommonRxTask<T, R>> onSubscribe = getCommonRxTaskOnSubscribe(rxTask);
        return Observable.create(onSubscribe)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<CommonRxTask<T, R>>() {
                    @Override
                    public void call(CommonRxTask<T, R> commonRxTask) {
                        commonRxTask.doInUIThread(commonRxTask.getOutData());  //在UI线程工作
                    }
                }, errorAction);
    }

    @NonNull
    private static <T, R> RxTaskOnSubscribe<CommonRxTask<T, R>> getCommonRxTaskOnSubscribe(final CommonRxTask<T, R> rxTask) {
        return new RxTaskOnSubscribe<CommonRxTask<T, R>>(rxTask) {
            @Override
            public void call(Subscriber<? super CommonRxTask<T, R>> subscriber) {
                CommonRxTask<T, R> task = getTask();
                task.setOutData(task.doInIOThread(task.getInData()));  //在io线程工作
                subscriber.onNext(task);
                subscriber.onCompleted();
            }
        };
    }

    //========================轮询操作==========================//
    /**
     * 轮询操作
     *
     * @param interval 轮询间期
     * @param action1  监听事件
     */
    public static Subscription polling(long interval, Action1 action1) {
        return polling(0, interval, action1);
    }

    /**
     * 轮询操作
     *
     * @param initialDelay 初始延迟
     * @param interval     轮询间期
     * @param action1      监听事件
     */
    public static Subscription polling(long initialDelay, long interval, Action1 action1) {
        return polling(initialDelay, interval, TimeUnit.SECONDS, action1, new SimpleThrowableAction(TAG));
    }

    /**
     * 轮询操作
     *
     * @param initialDelay 初始延迟
     * @param interval     轮询间期
     * @param unit         轮询间期时间单位
     * @param action1      监听事件
     * @param errorAction  出错的事件
     */
    public static Subscription polling(long initialDelay, long interval, TimeUnit unit, Action1 action1, Action1<Throwable> errorAction) {
        return Observable.interval(initialDelay, interval, unit)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(action1, errorAction);
    }

    //========================延迟操作==========================//
    /**
     * 延迟操作
     *
     * @param delayTime 延迟时间
     * @param action1   监听事件
     */
    public static Subscription delay(long delayTime, Action1 action1) {
        return delay(delayTime, TimeUnit.SECONDS, action1, new SimpleThrowableAction(TAG));
    }

    /**
     * 延迟操作
     *
     * @param delayTime   延迟时间
     * @param unit        延迟时间单位
     * @param action1     监听事件
     * @param errorAction 出错的事件
     */
    public static Subscription delay(long delayTime, TimeUnit unit, Action1 action1, Action1<Throwable> errorAction) {
        return Observable.timer(delayTime, unit)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(action1, errorAction);
    }

    /**
     * 遍历数组
     *
     * @param t       数组
     * @param action1 动作
     * @return
     */
    public static <T> Subscription foreach(T[] t, Action1<T> action1) {
        return Observable.from(t).subscribe(action1, new SimpleThrowableAction(TAG));
    }

    /**
     * 遍历集合
     *
     * @param t       数组
     * @param action1 动作
     * @return
     */
    public static <T> Subscription foreach(Iterable<T> t, Action1<T> action1) {
        return Observable.from(t).subscribe(action1, new SimpleThrowableAction(TAG));
    }

}
