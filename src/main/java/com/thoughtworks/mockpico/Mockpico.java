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
import org.picocontainer.annotations.Inject;
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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.picocontainer.injectors.Injectors.CDI;
import static org.picocontainer.injectors.Injectors.SDI;

public class Mockpico {

    public static final Class<? extends Annotation> JSR330_ATINJECT = getInjectionAnnotation("javax.inject.Inject");
    public static final Class<? extends Annotation> GUICE_ATINJECT = getInjectionAnnotation("com.google.Inject");
    public static final Class<? extends Annotation> SPRING_AUTOWIRED = getInjectionAnnotation("org.springframework.beans.factory.annotation.Autowired");

    private static final InjectionType[] DEFAULT_INJECTION_TYPES = new InjectionType[] {CDI(),
            new AnnotatedFieldInjection(Inject.class, JSR330_ATINJECT, SPRING_AUTOWIRED, GUICE_ATINJECT),
            new AnnotatedMethodInjection(false, Inject.class, JSR330_ATINJECT, SPRING_AUTOWIRED, GUICE_ATINJECT)};

    public static <T> ContainerOrInjectionTypesOrInjecteesOrJournalOrMakeNext<T> mockDepsFor(Class<T> type) {
        return new ContainerOrInjectionTypesOrInjecteesOrJournalOrMakeNext<T>(type);
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

    public static class JournalOrMakeNext<T> {

        protected final Class<T> type;
        protected final MutablePicoContainer mocks;
        protected final Object[] injectees;
        protected Journal journal = new Journal();

        private JournalOrMakeNext(Class<T> type, MutablePicoContainer mocks, Object[] injectees) {
            this.type = type;
            this.mocks = mocks;
            this.injectees = injectees;
        }

        public JournalOrMakeNext<T> journalTo(Journal journal) {
            this.journal = journal;
            return this;
        }

        public T make() {
            mocks.changeMonitor(new MockpicoComponentMonitor(journal));
            for (Object injectee : injectees) {
                String s = injectee.getClass().getName();
                if (s.indexOf("ByMockito") > -1) {
                    Class<?> parent = injectee.getClass().getSuperclass();
                    if (parent == Object.class) {
                        parent = injectee.getClass().getInterfaces()[0];
                    }
                    mocks.addComponent(parent, injectee);
                } else {
                    mocks.addComponent(injectee);
                }
            }
            return mocks.addComponent(type).getComponent(type);
        }
    }

    public static class InjecteesOrJournalOrMakeNext<T> extends JournalOrMakeNext<T> {

        private InjecteesOrJournalOrMakeNext(Class<T> type, MutablePicoContainer mocks, Object[] injectees) {
            super(type, mocks, injectees);
        }

        private InjecteesOrJournalOrMakeNext(Class<T> type) {
            super(type, makePicoContainer(DEFAULT_INJECTION_TYPES), new Object[0]);
        }

        public JournalOrMakeNext<T> withInjectees(Object... injectees) {
            return new JournalOrMakeNext<T>(type, mocks, injectees);
        }

    }

    public static class ContainerOrInjectionTypesOrInjecteesOrJournalOrMakeNext<T> extends InjecteesOrJournalOrMakeNext<T> {

        private ContainerOrInjectionTypesOrInjecteesOrJournalOrMakeNext(Class<T> type) {
            super(type);
        }

        public InjecteesOrJournalOrMakeNext<T> using(MutablePicoContainer mocks) {
            return new InjecteesOrJournalOrMakeNext<T>(type, mocks, new Object[0]);
        }

        public InjecteesOrJournalOrMakeNext<T> withInjectionTypes(InjectionType... injectionFactories) {
            return using(makePicoContainer(injectionFactories));
        }

        public InjecteesOrJournalOrMakeNext<T> withSetters() {
            List<InjectionType> injectionTypes = new ArrayList<InjectionType>(Arrays.asList(DEFAULT_INJECTION_TYPES));
            injectionTypes.add(SDI());
            return withInjectionTypes(injectionTypes.toArray(new InjectionType[injectionTypes.size()]));
        }
    }

    public static MutablePicoContainer makePicoContainer() {
        return makePicoContainer(new EmptyPicoContainer());
    }

    public static MutablePicoContainer makePicoContainer(PicoContainer parent) {
        return makePicoContainer(parent, DEFAULT_INJECTION_TYPES);
    }

    public static MutablePicoContainer makePicoContainer(InjectionType... injectionFactories) {
        return makePicoContainer(new EmptyPicoContainer(), injectionFactories);
    }

    public static MutablePicoContainer makePicoContainer(PicoContainer parent, InjectionType... injectionFactories) {
        return new DefaultPicoContainer(parent, new NullLifecycleStrategy(), new NullComponentMonitor(),
                new Caching().wrap(new CompositeInjection(injectionFactories)));
    }

    private static Class<? extends Annotation> getInjectionAnnotation(String className) {
        try {
            return (Class<? extends Annotation>) Mockpico.class.getClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            // JSR330 or Spring not in classpath.  No matter carry on without it with a kludge:
            return org.picocontainer.annotations.Inject.class;
        }
    }

    private static class MockpicoComponentMonitor extends NullComponentMonitor {

        private final Journal journal;

        private MockpicoComponentMonitor(Journal journal) {
            this.journal = journal;
        }

        @Override
        public <T> void instantiated(PicoContainer pico, ComponentAdapter<T> componentAdapter, Constructor<T> constructor,
                                     Object instantiated, Object[] injected, long duration) {
            journal.append("Constructor being injected:\n");
            super.instantiated(pico, componentAdapter, constructor, instantiated, injected, duration);
            for (int i = 0; i < injected.length; i++) {
                journal.append(new Journal.Arg(i, constructor.getParameterTypes()[i], injected[i]));
            }
        }

        @Override
        public <T> void instantiationFailed(PicoContainer container, ComponentAdapter<T> componentAdapter,
                                            Constructor<T> constructor, Exception e) {
            super.instantiationFailed(container, componentAdapter, constructor, e);    
        }

        @Override
        public void invoked(PicoContainer pico, ComponentAdapter<?> componentAdapter, Member member, Object instance,
                            long duration, Object retVal, Object... args) {
            super.invoked(pico, componentAdapter, member, instance, duration, retVal, args);
            if (member instanceof Method) {
                Method method = (Method) member;
                journal.append("Method '" + method.getName() + "' being injected: \n");
                for (int i = 0; i < args.length; i++) {
                    journal.append(new Journal.Arg(i, method.getParameterTypes()[i], args[i]));
                }
            } else {
                journal.append(new Journal.Field(member, args[0]));
            }
        }

        @Override
        public Object noComponentFound(MutablePicoContainer pico, Object classToMock) {
            if (classToMock instanceof Class) {
                if (classToMock == Integer.class) {
                    return 0;
                } else if (classToMock == Long.class) {
                    return (long) 0;
                } else if (classToMock == Double.class) {
                    return (double) 0;
                } else if (classToMock == Byte.class) {
                    return (byte) 0;
                } else if (classToMock == Short.class) {
                    return (short) 0;
                } else if (classToMock == Float.class) {
                    return (float) 0;
                } else if (classToMock == Boolean.class) {
                    return false;
                } else if (classToMock == Character.class) {
                    return (char) 0;
                } else if (classToMock == String.class) {
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
