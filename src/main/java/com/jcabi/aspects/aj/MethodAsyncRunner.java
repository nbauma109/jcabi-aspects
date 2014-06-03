/**
 * Copyright (c) 2012-2014, jcabi.com
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met: 1) Redistributions of source code must retain the above
 * copyright notice, this list of conditions and the following
 * disclaimer. 2) Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided
 * with the distribution. 3) Neither the name of the jcabi.com nor
 * the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jcabi.aspects.aj;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

/**
 * Execute method asynchronously.
 *
 * <p>It is an AspectJ aspect and you are not supposed to use it directly. It
 * is instantiated by AspectJ runtime framework when your code is annotated
 * with {@link com.jcabi.aspects.Async} annotation.
 *
 * @author Carlos Miranda (miranda.cma@gmail.com)
 * @version $Id$
 * @since 0.16
 */
@Aspect
public final class MethodAsyncRunner {

    /**
     * Thread pool for asynchronous execution.
     */
    private final transient ExecutorService executor =
        Executors.newCachedThreadPool(
            new NamedThreads(
                "async",
                "Asynchronous method execution"
            )
        );

    /**
     * Execute method asynchronously.
     *
     * <p>This aspect should be used only on {@code void} or
     * {@link java.util.concurrent.Future} returning methods.
     *
     * <p>Try NOT to change the signature of this method, in order to keep
     * it backward compatible.
     *
     * @param point Joint point
     * @return The result of call
     */
    @Around("execution(@com.jcabi.aspects.Async * * (..))")
    @SuppressWarnings("PMD.AvoidCatchingThrowable")
    public Object wrap(final ProceedingJoinPoint point) {
        final Future<?> result = this.executor.submit(
            // @checkstyle AnonInnerLength (23 lines)
            new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    Object returned = null;
                    try {
                        final Object result = point.proceed();
                        if (result instanceof Future) {
                            returned = ((Future<?>) result).get();
                        }
                    // @checkstyle IllegalCatch (1 line)
                    } catch (final Throwable ex) {
                        throw new IllegalStateException(
                            String.format(
                                "%s: Exception thrown",
                                Mnemos.toText(point, true, true)
                            ),
                            ex
                        );
                    }
                    return returned;
                }
            }
        );
        Object res = null;
        if (Future.class.isAssignableFrom(
            MethodSignature.class.cast(point.getSignature())
                .getMethod().getReturnType()
        )) {
            res = result;
        }
        return res;
    }

}
