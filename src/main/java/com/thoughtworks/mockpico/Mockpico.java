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
import org.picocontainer.InjectionType;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.PicoContainer;
import org.picocontainer.adapters.InstanceAdapter;
import org.picocontainer.behaviors.Caching;
import org.picocontainer.containers.EmptyPicoContainer;
import org.picocontainer.injectors.AnnotatedFieldInjection;
import org.picocontainer.injectors.AnnotatedMethodInjection;
import org.picocontainer.injectors.CompositeInjection;
import org.picocontainer.lifecycle.NullLifecycleStrategy;
import org.picocontainer.monitors.NullComponentMonitor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.util.Collection;

import static org.picocontainer.injectors.Injectors.CDI;
import static org.picocontainer.injectors.Injectors.SDI;

public class Mockpico {

    public static final Class<? extends Annotation> PICO_ATINJECT = org.picocontainer.annotations.Inject.class;
    public static final Class<? extends Annotation> JSR330_ATINJECT = getInjectionAnnotation("javax.inject.Inject");
    public static final Class<? extends Annotation> AUTOWIRED = getInjectionAnnotation("org.springframework.beans.factory.annotation.Autowired");

    public static <T> ContainerToDo<T> mockDepsFor(Class<T> type) {
        return new ContainerToDo<T>(type);
    }

    public static void verifyNoMoreInteractionsForAll(PicoContainer mocks) {
        Collection<ComponentAdapter<?>> foo = mocks.getComponentAdapters();
        for (ComponentAdapter<?> componentAdapter : foo) {
            InstanceAdapter ia = componentAdapter.findAdapterOfType(InstanceAdapter.class);
            if (ia != null && ia.getComponentImplementation().getName().indexOf("EnhancerByMockitoWithCGLIB") > 0) {
                Mockito.verifyNoMoreInteractions(ia.getComponentInstance(mocks, ComponentAdapter.NOTHING.class));
            }
        }
    }

    public static void resetAll(MutablePicoContainer mocks) {
        Collection<ComponentAdapter<?>> foo = mocks.getComponentAdapters();
        for (ComponentAdapter<?> componentAdapter : foo) {
            InstanceAdapter ia = componentAdapter.findAdapterOfType(InstanceAdapter.class);
            if (ia != null && ia.getComponentImplementation().getName().indexOf("EnhancerByMockitoWithCGLIB") > 0) {
                Mockito.reset(ia.getComponentInstance(mocks, ComponentAdapter.NOTHING.class));
            }
        }
    }

    public static class MakeToDo<T> {
        protected final Class<T> type;
        protected final MutablePicoContainer mocks;
        protected final Object[] injectees;

        public MakeToDo(Class<T> type) {
            this(type, makePicoContainer(), new Object[0]);
        }
        public MakeToDo(Class<T> type, MutablePicoContainer mocks, Object[] injectees) {
            this.type = type;
            this.mocks = mocks;
            this.injectees = injectees;
        }

        public T make() {
            mocks.addComponent(new Journal());
            for (Object extra : injectees) {
                String s = extra.getClass().getName();
                if (s.indexOf("ByMockito") > -1) {
                    Class<?> parent = extra.getClass().getSuperclass();
                    if (parent == Object.class) {
                        parent = extra.getClass().getInterfaces()[0];
                    }
                    mocks.addComponent(parent, extra);
                } else {
                    mocks.addComponent(extra);
                }
            }
            return mocks.addComponent(type).getComponent(type);
        }
    }

    public static class InjecteesToDo<T> extends MakeToDo<T> {
        public InjecteesToDo(Class<T> type, MutablePicoContainer mocks, Object[] injectees) {
            super(type, mocks, injectees);
        }

        public InjecteesToDo(Class<T> type) {
            super(type, makePicoContainer(CDI(), new AnnotatedFieldInjection(PICO_ATINJECT, JSR330_ATINJECT, AUTOWIRED),
                    new AnnotatedMethodInjection(false, PICO_ATINJECT, JSR330_ATINJECT, AUTOWIRED)), new Object[0]);
        }

        public MakeToDo<T> withInjectees(Object... injectees) {
            return new MakeToDo<T>(type, mocks, injectees);
        }

    }

    public static class ContainerToDo<T> extends InjecteesToDo<T> {
        public ContainerToDo(Class<T> type) {
            super(type);
        }

        public InjecteesToDo<T> using(MutablePicoContainer mocks) {
            return new InjecteesToDo<T>(type, mocks, new Object[0]);
        }

        public InjecteesToDo<T> withInjectionTypes(InjectionType... injectionFactories) {
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

    public static MutablePicoContainer makePicoContainer(InjectionType... injectionFactories) {
        return makePicoContainer(new EmptyPicoContainer(), injectionFactories);
    }

    public static MutablePicoContainer makePicoContainer(PicoContainer parent, InjectionType... injectionFactories) {
        return new DefaultPicoContainer(parent, new NullLifecycleStrategy(), new MockitoComponentMonitor(), new Caching().wrap(new CompositeInjection(injectionFactories)));
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
                if (classToMock == Integer.class) {
                    return 0;
                } else if (classToMock == Long.class){
                    return (long) 0;
                } else if (classToMock == Double.class){
                    return (double) 0;
                } else if (classToMock == Byte.class){
                    return (byte) 0;
                } else if (classToMock == Short.class){
                    return (short) 0;
                } else if (classToMock == Float.class){
                    return (float) 0;
                } else if (classToMock == Boolean.class){
                    return false;
                } else if (classToMock == Character.class){
                    return (char) 0;
                } else if (classToMock == String.class){
                    return "";
                } else {
                    Object mocked = Mockito.mock((Class) classToMock);
                    pico.addComponent(classToMock, mocked);
                    return mocked;
                }
            }
            return null;
        }
    }
}
