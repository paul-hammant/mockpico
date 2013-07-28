What is Mockpico?
=================

Mockpico helps test components with many injectable dependencies. If some or most of those dependencies are unimportant to the actual behavior you are trying to test, then Mockpico makes the setup easier.

A Simple Example
----------------

Here is a simple example. Say FooController has a dozen dependencies injected in through the constructor, setters and appropriately annotated fields, but you just don’t care about most of the deps, the Mockpico can automatically inject mocks for them:

    FooController fc = mockDepsFor(FooController.class).withInjectees(a, b, c, d).make();

The above would be instead of this below (or worse)

    FooController fc = new FooController(a, b, null, null, null, null, null, null);
    fc.setC(c);
    fc.d = d; // assuming the field 'd' is not private

The injectees above could be real components, or Mockito mocks (your choice).  They can be specified in any order as PicoContainer will just work the right order.  Dependencies not specified will be mocked automatically.

Understands a few Dependency Injection (DI) technologies
--------------------------------------------------------

The setter methods or fields to inject into were annotated with Spring’s @Autowired or @Inject from JSR330, Guice or PicoContainer. The four variables in this case (a, b, c, d) are injected into where the applicable injection-points as declared for FooController. Each of the four could be Mocks made by Mockito, or real or stub implementations as you see fit.

Here is a full test case showing a number of permutations of Mockpico in use:
[MockpicoTestCase.java](mockpico/blob/master/src/test/java/com/thoughtworks/mockpico/MockpicoTestCase.java).

Do you have too many injected dependencies?
-------------------------------------------

A word of warning, to folks embracing DI more than decomposition:

![an injection diagram](http://paulhammant.com/images/injection-diag.png "Collaborators Are Better")

Where Mockpico facilitates good work
------------------------------------

If your ripple through your testbase getting rid of the use of controller's constructors, then you are able to change the types of injection easily. This is because the change only affects a single controller at a time, as the tests no longer directly manipulate constructors, setters of injectable fields.

Other Mockpico functionality
----------------------------

Showing what injectees were used or mocked for debugging purposes:

    Journal journal = new Journal();

    FooController fc = mockDepsFor(FooController.class)
       .journalTo(journal)
       .make();

    System.out.println(journal);

Adding in old-fashioned (unannotated) setter injection:
         
    FooController fc = mockDepsFor(FooController.class)
       .withSetters()
       .make();

Providing your own container for injectees (real and mock ones) :

    FooController fc = mockDepsFor(FooController.class)
       .using(makePicoContainer(CDI(), new AnnotatedMethodInjection(false, YourCustomInjectAnnotation.class)))
       .make();
