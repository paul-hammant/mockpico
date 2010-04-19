package com.thoughtworks.mockpico;
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

import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.exceptions.verification.NoInteractionsWanted;
import org.picocontainer.MutablePicoContainer;
import org.springframework.beans.factory.annotation.Autowired;

import javax.inject.Inject;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.math.BigInteger;
import java.util.List;

import static com.sun.tools.internal.ws.wsdl.parser.Util.fail;
import static com.thoughtworks.mockpico.Mockpico.injectionAnnotation;
import static com.thoughtworks.mockpico.Mockpico.makePicoContainer;
import static com.thoughtworks.mockpico.Mockpico.mockDepsFor;
import static com.thoughtworks.mockpico.Mockpico.resetAll;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.picocontainer.injectors.Injectors.CDI;

public class MockpicoTestCase {

    private AnotherThing anotherThing = new AnotherThing();
    private YetAnotherThing yetAnotherThing = new YetAnotherThing();
    private Thing thing = new Thing(anotherThing);

    @Test
    public void testCanMockConstructorAndSetterDeps() {

        BigCheese bc = mockDepsFor(BigCheese.class)
                .withSetters()
                .make();

        assertNotNull(bc);
        assertNotNull(bc.thingViaCtor);
        assertIsAMock(bc.thingViaCtor);
        assertNotSame(Thing.class, bc.thingViaCtor.getClass());
        assertNotNull(bc.anotherThingViaCtor);
        assertIsAMock(bc.anotherThingViaCtor);
        assertNotSame(AnotherThing.class, bc.anotherThingViaCtor.getClass());
        assertNotNull(bc.yetAnotherThingViaSetter);
        assertIsAMock(bc.yetAnotherThingViaSetter);
        assertNotSame(YetAnotherThing.class, bc.yetAnotherThingViaSetter.getClass());
    }

    @Test
    public void testCanSupplyConstructorAndSetterDeps() {

        BigCheese bc = mockDepsFor(BigCheese.class)
                .withSetters()
                .withInjectees(thing, anotherThing, yetAnotherThing)
                .make();

        assertNotNull(bc);
        constructorInjectedItemsAreReal(bc);
        setterInjectedItemsAreReal(bc);
    }

    @Test
    public void testPicoCanMakeAndCacheRealConstructorAndSetterDeps() {

        BigCheese bc = mockDepsFor(BigCheese.class)
                .withSetters()
                .withInjectees(Thing.class, AnotherThing.class, YetAnotherThing.class)
                .make();

        assertNotNull(bc);
        constructorInjectedItemsAreInjected(bc);
        setterInjectedItemsAreInjected(bc);
    }

    @Test
    public void testCanSkipSettersIfNotSpecified() {

        BigCheese bc = mockDepsFor(BigCheese.class)
                .withInjectees(thing, anotherThing)
                .make();

        assertNotNull(bc);
        constructorInjectedItemsAreReal(bc);
        assertNull(bc.yetAnotherThingViaSetter);
        atInjectItemsAreReal(bc);
        autowiredItemsAreReal(bc);
    }


    @Test
    public void testCanSupplyConstructorDepsOnly() {

        BigCheese bigCheese = mockDepsFor(BigCheese.class)
                .withInjectionTypes(CDI())
                .withInjectees(thing, anotherThing, yetAnotherThing)
                .make();

        assertNotNull(bigCheese);
        constructorInjectedItemsAreReal(bigCheese);
        assertNull(bigCheese.yetAnotherThingViaSetter);
        assertNull(bigCheese.thingViaAtInject);
        assertNull(bigCheese.thingViaAutowired);
    }

    @Test
    public void testCanPopulateAPicoHandedInAndJournalInjections() {
        MutablePicoContainer pico = makePicoContainer();

        BigCheese bc = mockDepsFor(BigCheese.class)
                .using(pico)
                .make();

        assertNotNull(bc);
        constructorInjectedItemsAreMock(bc);
        setterInjectedItemsAreMock(bc);

        assertSame(pico.getComponent(Thing.class), bc.thingViaCtor);
        assertSame(pico.getComponent(AnotherThing.class), bc.anotherThingViaCtor);
        assertSame(pico.getComponent(YetAnotherThing.class), bc.yetAnotherThingViaSetter);

        String actual = pico.getComponent(Mockpico.Journal.class).toString()
                .replace(""+System.identityHashCode(bc.anotherThingViaCtor), "ONE")
                .replace(""+System.identityHashCode(bc.thingViaCtor), "TWO")
                .replace(""+System.identityHashCode(bc.yetAnotherThingViaSetter), "THREE");
        assertEquals("Constructor being injected:\n" +
                "  arg[0] type:class com.thoughtworks.mockpico.MockpicoTestCase$AnotherThing, with: Mock for AnotherThing, hashCode: ONE\n" +
                "  arg[1] type:class com.thoughtworks.mockpico.MockpicoTestCase$Thing, with: Mock for Thing, hashCode: TWO\n" +
                "Method being injected: 'setQwertyAsdfgh' with: Mock for YetAnotherThing, hashCode: THREE\n", actual);
    }

    @Test
    public void testCanMockConstructorAndInjectees() {

        BigCheese bc = mockDepsFor(BigCheese.class)
                .make();

        constructorInjectedItemsAreMock(bc);
        assertNull(bc.yetAnotherThingViaSetter);
        autowiredItemsAreMock(bc);
        atInjectItemsAreMock(bc);
    }

    @Test
    public void testCanMockPrimivitesAndUseCustomDifferentAnnotation() {

        BigCheese bc = mockDepsFor(BigCheese.class)
                .using(makePicoContainer(CDI(), injectionAnnotation(BigCheese.Foobarred.class)))
                .make();

        customAnnotationItemsAreRandom(bc);
    }

    @Test
    public void verifyNoMoreInteractionsCanBePercolated() {
        MutablePicoContainer mocks = makePicoContainer();

        NeedsList nl = mockDepsFor(NeedsList.class)
                .using(mocks)
                .make();

        nl.oops();
        try {
            Mockpico.verifyNoMoreInteractionsForAll(mocks);
            fail("should have barfed");
        } catch (NoInteractionsWanted e) {
            // expected  
        }
    }

    @Test
    public void resetCanBePercolated() {
        MutablePicoContainer mocks = makePicoContainer();

        NeedsList nl = mockDepsFor(NeedsList.class)
                .using(mocks)
                .make();

        nl.oops();
        resetAll(mocks);
        Mockito.verifyNoMoreInteractions(mocks.getComponent(List.class));
    }

    public static class NeedsList {
        private List list;

        public NeedsList(List list) {
            this.list = list;
        }

        public void oops() {
            list.add("oops");
        }

    }

    public static class BigCheese {
        private final AnotherThing anotherThingViaCtor;
        private final Thing thingViaCtor;
        private YetAnotherThing yetAnotherThingViaSetter;
        private Thing thingViaAtInject;
        private Thing thingViaAutowired;
        private String nameViaCustomAnnotation;
        private int ageViaCustomAnnotation;
        private double doubleViaCustomAnnotation;
        private Double double2ViaCustomAnnotation;
        private boolean booleanViaCustomAnnotation;
        private float floatViaCustomAnnotation;
        private byte byteViaCustomAnnotation;
        private short shortViaCustomAnnotation;
        private BigInteger bigIntViaCustomAnnotation;
        private char charViaCustomAnnotation;
        private Long longViaCustomAnnotation;

        public BigCheese(AnotherThing anotherThing, Thing thing) {
            this.anotherThingViaCtor = anotherThing;
            this.thingViaCtor = thing;
        }

        public void setQwertyAsdfgh(YetAnotherThing yetAnotherThing) {
            this.yetAnotherThingViaSetter = yetAnotherThing;
        }

        @Inject
        public void andCrackers(Thing thing) {
            this.thingViaAtInject = thing;
        }

        @Autowired
        public void andWine(Thing thing) {
            this.thingViaAutowired = thing;
        }

        @Foobarred
        public void foobar(String name, int age,
                           double doubleViaCustomAnnotation,
                           Double double2ViaCustomAnnotation,
                           boolean booleanViaCustomAnnotation,
                           float floatViaCustomAnnotation,
                           byte byteViaCustomAnnotation,
                           short shortViaCustomAnnotation,
                           BigInteger bigIntViaCustomAnnotation,
                           char charViaCustomAnnotation,
                           Long longViaCustomAnnotation) {
            this.nameViaCustomAnnotation = name;
            this.ageViaCustomAnnotation = age;
            this.doubleViaCustomAnnotation = doubleViaCustomAnnotation;
            this.double2ViaCustomAnnotation = double2ViaCustomAnnotation;
            this.booleanViaCustomAnnotation = booleanViaCustomAnnotation;
            this.floatViaCustomAnnotation = floatViaCustomAnnotation;
            this.byteViaCustomAnnotation = byteViaCustomAnnotation;
            this.shortViaCustomAnnotation = shortViaCustomAnnotation;
            this.bigIntViaCustomAnnotation = bigIntViaCustomAnnotation;
            this.charViaCustomAnnotation = charViaCustomAnnotation;
            this.longViaCustomAnnotation = longViaCustomAnnotation;
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

    private void autowiredItemsAreMock(BigCheese bc) {
        assertNotNull(bc.thingViaAutowired);
        assertIsAMock(bc.thingViaAutowired);
    }

    private void customAnnotationItemsAreRandom(BigCheese bc) {
        assertEquals("", bc.nameViaCustomAnnotation);
        assertEquals(0, bc.ageViaCustomAnnotation);
        assertEquals(0.0, bc.doubleViaCustomAnnotation);
        assertEquals(0.0, bc.double2ViaCustomAnnotation);
        assertEquals(0, bc.byteViaCustomAnnotation);
        assertEquals(0, bc.shortViaCustomAnnotation);
        assertEquals((float) 0.0, bc.floatViaCustomAnnotation);
        assertEquals(false, bc.booleanViaCustomAnnotation);
        assertEquals(0, bc.charViaCustomAnnotation);
        assertEquals(new Long(0), bc.longViaCustomAnnotation);
    }

    private void atInjectItemsAreMock(BigCheese bc) {
        assertNotNull(bc.thingViaAtInject);
        assertIsAMock(bc.anotherThingViaCtor);

    }

    private void setterInjectedItemsAreInjected(BigCheese bc) {
        assertNotNull(bc.yetAnotherThingViaSetter);
    }

    private void setterInjectedItemsAreReal(BigCheese bc) {
        setterInjectedItemsAreInjected(bc);
        assertSame(YetAnotherThing.class, bc.yetAnotherThingViaSetter.getClass());
    }

    private void setterInjectedItemsAreMock(BigCheese bc) {
        setterInjectedItemsAreInjected(bc);
        assertIsAMock(bc.yetAnotherThingViaSetter);
    }

    private void assertIsAMock(Object obj) {
        assertTrue(isAMock(obj));
    }

    private boolean isAMock(Object obj) {
        return obj.getClass().getName().indexOf("EnhancerByMockitoWithCGLIB") > 0;
    }

    private void autowiredItemsAreReal(BigCheese bc) {
        assertNotNull(bc.thingViaAutowired);
        assertSame(Thing.class, bc.thingViaAutowired.getClass());
    }

    private void atInjectItemsAreReal(BigCheese bc) {
        assertNotNull(bc.thingViaAtInject);
        assertSame(Thing.class, bc.thingViaAtInject.getClass());
    }

    private void constructorInjectedItemsAreReal(BigCheese bc) {
        constructorInjectedItemsAreInjected(bc);
        assertSame(thing, bc.thingViaCtor);
        assertSame(Thing.class, bc.thingViaCtor.getClass());
        assertSame(anotherThing, bc.anotherThingViaCtor);
        assertSame(AnotherThing.class, bc.anotherThingViaCtor.getClass());
        assertSame(bc.anotherThingViaCtor, bc.thingViaCtor.anotherThing);

    }

    private void constructorInjectedItemsAreMock(BigCheese bc) {
        constructorInjectedItemsAreInjected(bc);
        assertIsAMock(bc.thingViaCtor);
        assertIsAMock(bc.anotherThingViaCtor);
    }

    private void constructorInjectedItemsAreInjected(BigCheese bc) {
        assertNotNull(bc.thingViaCtor);
        assertNotNull(bc.anotherThingViaCtor);
    }



}
