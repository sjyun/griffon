/*
 * Copyright 2010-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package griffon.test.mock;

import griffon.util.UIThreadHandler;

/**
 * Trivial implementation of {@code UIThreadHandler} that runs code in the same thread
 * as the caller.
 *
 * @author Andres Almiray
 */
public class MockUIThreadHandler implements UIThreadHandler {
    public boolean isUIThread() {
        return false;
    }

    public void executeAsync(Runnable runnable) {
        runnable.run();
    }

    public void executeSync(Runnable runnable) {
        runnable.run();
    }

    public void executeOutside(Runnable runnable) {
        runnable.run();
    }
}