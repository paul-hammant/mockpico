/**
 * Copyright (c) 2010 ThoughtWorks
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

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;

import static org.picocontainer.injectors.Injectors.CDI;
import static org.picocontainer.injectors.Injectors.SDI;

public class Mockpico {

    public static final InjectionFactory PICO_ATINJECT = injectionAnnotation(org.picocontainer.annotations.Inject.class);
    public static final InjectionFactory JSR330_ATINJECT = injectionAnnotation(getInjectionAnnotation("javax.inject.Inject"));
    public static final InjectionFactory AUTOWIRED = injectionAnnotation(getInjectionAnnotation("org.springframework.beans.factory.annotation.Autowired"));

    public static <T> ContainerToDo<T> mockDepsFor(Class<T> type) {
        return new ContainerToDo<T>(type);
    }

    public static class MakeToDo<T> {
        protected final Class<T> type;
        protected final MutablePicoContainer mutablePicoContainer;
        protected final Object[] injectees;

        public MakeToDo(Class<T> type) {
            this(type, makePicoContainer(), new Object[0]);
        }
        public MakeToDo(Class<T> type, MutablePicoContainer mutablePicoContainer, Object[] injectees) {
            this.type = type;
            this.mutablePicoContainer = mutablePicoContainer;
            this.injectees = injectees;
        }

        public T make() {
            mutablePicoContainer.addComponent(new Journal());
            for (Object extra : injectees) {
                mutablePicoContainer.addComponent(extra);
            }
            return mutablePicoContainer.addComponent(type).getComponent(type);

        }
    }

    public static class InjecteesToDo<T> extends MakeToDo<T> {
        public InjecteesToDo(Class<T> type, MutablePicoContainer mutablePicoContainer, Object[] injectees) {
            super(type, mutablePicoContainer, injectees);
        }

        public InjecteesToDo(Class<T> type) {
            super(type, makePicoContainer(CDI(), PICO_ATINJECT, JSR330_ATINJECT, AUTOWIRED), new Object[0]);
        }

        public MakeToDo<T> withInjectees(Object... injectees) {
            return new MakeToDo<T>(type, mutablePicoContainer, injectees);
        }

    }

    public static class ContainerToDo<T> extends InjecteesToDo<T> {
        public ContainerToDo(Class<T> type) {
            super(type);
        }

        public InjecteesToDo<T> using(MutablePicoContainer mutablePicoContainer) {
            return new InjecteesToDo<T>(type, mutablePicoContainer, new Object[0]);
        }

        public InjecteesToDo<T> withInjectionTypes(InjectionFactory... injectionFactories) {
            return using(makePicoContainer(injectionFactories));
        }

        public InjecteesToDo<T> withSetters() {
            return using(makePicoContainer(CDI(), SDI()));
        }
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

    public static InjectionFactory injectionAnnotation(Class<? extends Annotation> annotation) {
        return new AnnotatedMethodInjection(annotation, false);
    }

    private static Class<? extends Annotation> getInjectionAnnotation(String className) {
        try {
            return (Class<? extends Annotation>) Mockpico.class.getClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            // JSR330 or Spring not in classpath.  No matter carry on without it with a kludge:
            return org.picocontainer.annotations.Inject.class;
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
        public <T> void instantiationFailed(PicoContainer container, ComponentAdapter<T> componentAdapter, Constructor<T> constructor, Exception e) {
            super.instantiationFailed(container, componentAdapter, constructor, e);    
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
                if (classToMock == Integer.class) {
                    return (int) Math.random();
                } else if (classToMock == String.class){
                    return "random:" + Math.random();
                }
                Class toMock = (Class) classToMock;
                Object mocked = Mockito.mock(toMock);
                pico.addComponent(classToMock, mocked);
                return mocked;
            }
            return null;
        }
    }
}
