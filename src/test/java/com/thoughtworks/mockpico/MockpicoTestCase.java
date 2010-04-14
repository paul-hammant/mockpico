package com.thoughtworks.mockpico;
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

import org.junit.Test;
import org.picocontainer.MutablePicoContainer;
import org.springframework.beans.factory.annotation.Autowired;

import javax.inject.Inject;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static com.thoughtworks.mockpico.Mockpico.injectionAnnotation;
import static com.thoughtworks.mockpico.Mockpico.makePicoContainer;
import static com.thoughtworks.mockpico.Mockpico.mockDepsFor;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.picocontainer.injectors.Injectors.CDI;

public class MockpicoTestCase {

    @Test
    public void testCanMockConstructorAndSetterDeps() {
        BigCheese bc = mockDepsFor(BigCheese.class).withSetters().make();

        assertNotNull(bc);
        assertNotNull(bc.thing);
        assertIsAMock(bc.thing);
        assertNotSame(Thing.class, bc.thing.getClass());
        assertNotNull(bc.anotherThing);
        assertIsAMock(bc.anotherThing);
        assertNotSame(AnotherThing.class, bc.anotherThing.getClass());
        assertNotNull(bc.yetAnotherThing);
        assertIsAMock(bc.yetAnotherThing);
        assertNotSame(YetAnotherThing.class, bc.yetAnotherThing.getClass());
    }

    private void assertIsAMock(Object obj) {
        assertTrue(obj.getClass().getName().indexOf("EnhancerByMockitoWithCGLIB") > 0);
    }

    @Test
    public void testCanSupplyConstructorAndSetterDeps() {
        AnotherThing anotherThing = new AnotherThing();
        YetAnotherThing yetAnotherThing = new YetAnotherThing();
        Thing thing = new Thing(anotherThing);

        BigCheese bc = mockDepsFor(BigCheese.class).withSetters().withInjectees(thing, anotherThing, yetAnotherThing).make();

        assertNotNull(bc);
        assertSame(thing, bc.thing);
        assertSame(Thing.class, bc.thing.getClass());
        assertSame(anotherThing, bc.anotherThing);
        assertSame(AnotherThing.class, bc.anotherThing.getClass());
        assertSame(yetAnotherThing, bc.yetAnotherThing);
        assertSame(YetAnotherThing.class, bc.yetAnotherThing.getClass());
    }

    @Test
    public void testPicoCanMakeAndCacheRealConstructorAndSetterDeps() {
        BigCheese bc = mockDepsFor(BigCheese.class).withSetters().withInjectees(Thing.class, AnotherThing.class, YetAnotherThing.class).make();

        assertNotNull(bc);
        assertNotNull(bc.thing);
        assertSame(Thing.class, bc.thing.getClass());
        assertSame(bc.anotherThing, bc.thing.anotherThing);
        assertNotNull(bc.anotherThing);
        assertSame(AnotherThing.class, bc.anotherThing.getClass());
        assertNotNull(bc.yetAnotherThing);
        assertSame(YetAnotherThing.class, bc.yetAnotherThing.getClass());
    }

    @Test
    public void testCanSkipSettersIfNotSpecified() {
        AnotherThing anotherThing = new AnotherThing();
        Thing thing = new Thing(anotherThing);

        BigCheese bc = mockDepsFor(BigCheese.class).withInjectees(thing, anotherThing).make();

        assertNotNull(bc);
        assertNotNull(bc.thing);
        assertSame(Thing.class, bc.thing.getClass());
        assertSame(bc.anotherThing, bc.thing.anotherThing);
        assertNotNull(bc.anotherThing);
        assertSame(AnotherThing.class, bc.anotherThing.getClass());
    }

    @Test
    public void testCanPopulateAPicoHandedInAndJournalInjections() {
        MutablePicoContainer pico = makePicoContainer();

        BigCheese bc = mockDepsFor(BigCheese.class).using(pico).make();

        assertNotNull(bc);
        assertNotNull(bc.thing);
        assertIsAMock(bc.thing);
        assertSame(pico.getComponent(Thing.class), bc.thing);
        assertNotSame(Thing.class, bc.thing.getClass());
        assertNotNull(bc.anotherThing);
        assertIsAMock(bc.anotherThing);
        assertSame(pico.getComponent(AnotherThing.class), bc.anotherThing);
        assertNotSame(AnotherThing.class, bc.anotherThing.getClass());
        assertNotNull(bc.yetAnotherThing);
        assertIsAMock(bc.yetAnotherThing);
        assertSame(pico.getComponent(YetAnotherThing.class), bc.yetAnotherThing);
        assertNotSame(YetAnotherThing.class, bc.yetAnotherThing.getClass());

        String actual = pico.getComponent(Mockpico.Journal.class).toString()
                .replace(""+System.identityHashCode(bc.anotherThing), "ONE")
                .replace(""+System.identityHashCode(bc.thing), "TWO")
                .replace(""+System.identityHashCode(bc.yetAnotherThing), "THREE");
        assertEquals("Constructor being injected:\n" +
                "  arg[0] type:class com.thoughtworks.mockpico.MockpicoTestCase$AnotherThing, with: Mock for AnotherThing, hashCode: ONE\n" +
                "  arg[1] type:class com.thoughtworks.mockpico.MockpicoTestCase$Thing, with: Mock for Thing, hashCode: TWO\n" +
                "Method being injected: 'setQwertyAsdfgh' with: Mock for YetAnotherThing, hashCode: THREE\n", actual);
    }

    @Test
    public void testCanMockConstructorAndInjectees() {
        BigCheese bc = mockDepsFor(BigCheese.class).make();

        assertNotNull(bc);
        assertNotNull(bc.thing);
        assertIsAMock(bc.thing);
        assertNotSame(Thing.class, bc.thing.getClass());
        assertNotNull(bc.anotherThing);
        assertIsAMock(bc.anotherThing);
        assertNotSame(AnotherThing.class, bc.anotherThing.getClass());
        assertNull(bc.yetAnotherThing);
        assertNotNull(bc.crackers);
        assertIsAMock(bc.crackers);
        assertNotSame(Thing.class, bc.crackers.getClass());
        assertNotNull(bc.wine);
        assertIsAMock(bc.wine);
        assertNotSame(Thing.class, bc.wine.getClass());
    }

    @Test
    public void testCanMockPrimivitesWithCustomDifferentAnnotation() {
        BigCheese bc = mockDepsFor(BigCheese.class).using(makePicoContainer(CDI(), injectionAnnotation(BigCheese.Foobarred.class))).make();

        assertNotNull(bc.name);
        assertNotNull(bc.age);
    }

    public static class BigCheese {
        private final AnotherThing anotherThing;
        private final Thing thing;
        private YetAnotherThing yetAnotherThing;
        private Thing crackers;
        private Thing wine;
        private String name;
        private int age;

        public BigCheese(AnotherThing anotherThing, Thing thing) {
            this.anotherThing = anotherThing;
            this.thing = thing;
        }

        public void setQwertyAsdfgh(YetAnotherThing yetAnotherThing) {
            this.yetAnotherThing = yetAnotherThing;
        }

        @Inject
        public void andCrackers(Thing thing) {
            this.crackers = thing;
        }

        @Autowired
        public void andWine(Thing thing) {
            this.wine = thing;
        }

        @Foobarred
        public void foobar(String name, int age) {
            this.name = name;
            this.age = age;
        }

        @Retention(RetentionPolicy.RUNTIME)
        @Target({ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.METHOD})
        public static @interface Foobarred {
        }


    }

    public static class AnotherThing {
    }

    public static class YetAnotherThing {
    }

    public static class Thing {
        public final AnotherThing anotherThing;

        public Thing(AnotherThing anotherThing) {
            this.anotherThing = anotherThing;
        }
    }
}
