/**
 * Copyright (c) 2010 ThoughtWorks
 * Portions Copyright (c) 2007 Mockito Committers
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.thoughtworks.mockpico;

import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.picocontainer.ComponentAdapter;
import org.picocontainer.DefaultPicoContainer;
import org.picocontainer.InjectionFactory;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.PicoContainer;
import org.picocontainer.behaviors.Caching;
import org.picocontainer.containers.EmptyPicoContainer;
import org.picocontainer.injectors.AnnotatedMethodInjection;
import org.picocontainer.injectors.CompositeInjection;
import org.picocontainer.lifecycle.NullLifecycleStrategy;
import org.picocontainer.monitors.NullComponentMonitor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;

import static org.picocontainer.injectors.Injectors.CDI;
import static org.picocontainer.injectors.Injectors.SDI;

public class Mockpico {

    /**
     * Creates mock object of given class or interface.
     * <p/>
     * See examples in javadoc for {@link Mockito} class
     *
     * @param classToMock class or interface to mock
     * @return mock object
     */
    public static <T> T mock(Class<T> classToMock) {
        return Mockito.mock(classToMock);
    }

    /**
     * Specifies mock name. Naming mocks can be helpful for debugging - the name is used in all verification errors.
     * <p/>
     * Beware that naming mocks is not a solution for complex code which uses too many mocks or collaborators.
     * <b>If you have too many mocks then refactor the code</b> so that it's easy to test/debug without necessity of naming mocks.
     * <p/>
     * <b>If you use &#064;org.mockito.Mock annotation then you've got naming mocks for free!</b> &#064;Mock uses field name as mock name. {@link org.mockito.Mock Read more.}
     * <p/>
     * <p/>
     * See examples in javadoc for {@link Mockito} class
     *
     * @param classToMock class or interface to mock
     * @param name        of the mock
     * @return mock object
     */
    public static <T> T mock(Class<T> classToMock, String name) {
        return Mockito.mock(classToMock, name);
    }

    /**
     * Creates mock with a specified strategy for its answers to interactions.
     * It's quite advanced feature and typically you don't need it to write decent tests.
     * However it can be helpful when working with legacy systems.
     * <p>
     * It is the default answer so it will be used <b>only when you don't</b> stub the method call.
     * <p/>
     * <pre>
     *   Foo mock = mock(Foo.class, RETURNS_SMART_NULLS);
     *   Foo mockTwo = mock(Foo.class, new YourOwnAnswer());
     * </pre>
     * <p/>
     * <p>See examples in javadoc for {@link Mockito} class</p>
     *
     * @param classToMock   class or interface to mock
     * @param defaultAnswer default answer for unstubbed methods
     * @return mock object
     */
    public static <T> T mock(Class<T> classToMock, Answer defaultAnswer) {
        return Mockito.mock(classToMock, defaultAnswer);
    }

    public static InnerMockpico mockDeps(Object... extras) {
        return mockDeps(makePicoContainer(), extras);
    }

    public static InnerMockpico mockDeps(MutablePicoContainer pico, Object... extras) {
        return new InnerMockpico(pico, extras);
    }

    public static InnerMockpico mockInjectees(Object... extras) {
        return mockInjectees(makePicoContainer(CDI(), 
                new AnnotatedMethodInjection(org.picocontainer.annotations.Inject.class, false),
                new AnnotatedMethodInjection(javax.inject.Inject.class, false),
                new AnnotatedMethodInjection(org.springframework.beans.factory.annotation.Autowired.class, false)), extras);
    }

    public static InnerMockpico mockInjectees(MutablePicoContainer pico, Object... extras) {
        return new InnerMockpico(pico, extras);
    }

    public static MutablePicoContainer makePicoContainer() {
        return makePicoContainer(new EmptyPicoContainer());
    }

    public static MutablePicoContainer makePicoContainer(PicoContainer parent) {
        return makePicoContainer(parent, CDI(), SDI());
    }

    public static MutablePicoContainer makePicoContainer(InjectionFactory... injectionFactories) {
        return makePicoContainer(new EmptyPicoContainer(), injectionFactories);
    }

    public static MutablePicoContainer makePicoContainer(PicoContainer parent, InjectionFactory... injectionFactories) {
        return new DefaultPicoContainer(new Caching().wrap(new CompositeInjection(injectionFactories)), new NullLifecycleStrategy(), parent, new MockitoComponentMonitor());
    }


    public static class InnerMockpico {

        private final MutablePicoContainer pico;
        private final Object[] extras;

        public InnerMockpico(MutablePicoContainer pico, Object... extras) {
            this.pico = pico;
            this.extras = extras;
        }

        public <T> T on(Class<T> type) {
            pico.addComponent(new Journal());
            for (Object extra : extras) {
                pico.addComponent(extra);
            }
            return pico.addComponent(type).getComponent(type);
        }
    }

    public static class Journal {
        private StringBuilder builder = new StringBuilder();
        public void log(String line) {
            builder.append(line).append("\n");
        }
        @Override
        public String toString() {
            return builder.toString();
        }
    }

    private static class MockitoComponentMonitor extends NullComponentMonitor {
        @Override
        public <T> void instantiated(PicoContainer pico, ComponentAdapter<T> componentAdapter, Constructor<T> constructor, Object instantiated, Object[] injected, long duration) {
            Journal journal = pico.getComponent(Journal.class);
            journal.log("Constructor being injected:");
            super.instantiated(pico, componentAdapter, constructor, instantiated, injected, duration);
            for (int i = 0; i < injected.length; i++) {
                journal.log("  arg[" + i + "] type:" + constructor.getParameterTypes()[i] + ", with: " + injected[i].toString());
            }
        }

        @Override
        public void invoked(PicoContainer pico, ComponentAdapter<?> componentAdapter, Member member, Object instance, long duration, Object[] args, Object retVal) {
            Journal journal = pico.getComponent(Journal.class);
            super.invoked(pico, componentAdapter, member, instance, duration, args, retVal);
            journal.log("Method being injected: '" + member.getName() + "' with: " + args[0]);
        }

        @Override
        public Object noComponentFound(MutablePicoContainer pico, Object classToMock) {
            if (classToMock instanceof Class) {
// TODO primitives
//                if (classToMock == Integer.class) {
//                    return 0;
//                }
                Class toMock = (Class) classToMock;
                Object mocked = Mockito.mock(toMock);
                pico.addComponent(classToMock, mocked);
                return mocked;
            }
            return null;
        }
    }
}
