What is Mockpico?
=================

Mockpico helps test components with many injectable dependencies. If some or most of those dependencies are unimportant to the actual behavior you are trying to assert, then Mockpico can make the setup easier.

A Simple Example
----------------

Here is a simple example. Say FooController has a dozen dependencies injected in through the constructor, setters and appropriately annotated fields, but you just don’t care about most of the deps, the Mockpico can automatically inject mocks for them:

> FooController fc = mockDepsFor(FooController.class).withInjectees(a, b, c, d).make();

The injectees above could be real components, or Mockito mocks (your choice).  They can be specified in any order as PicoContainer will just work the right order.  Dependencies not specified will be mocked automatically.

Understands a few Dependency Injection (DI) technologies
--------------------------------------------------------

The setter methods or fields to inject into were annotated with Spring’s @Autowired or @Inject from JSR330, Guice or PicoContainer. The three variables in this case are injected into where there dependent types are declared for FooController. Each of the three could be Mocks made by Mockito, or real or stub implementations as you see fit.

Here is a full test case showing a number of permutations of Mockpico in use:
[MockpicoTestCase.java](mockpico/blob/master/src/test/java/com/thoughtworks/mockpico/MockpicoTestCase.java).

Do you have too many injected dependencies?
------------------------------------------

A word of warning, to folks embracing DI more than decomposition:

![alt injection diagram](mockpico/raw/master/src/graffle/injection-diag.png "Collaborators Are Better")