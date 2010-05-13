Mockito and PicoContainer have a fling. The result is a ugly child, that might be able to craft a living later in life.

Mockpico helps test components with many injectable dependencies. If some or most of those dependencies are unimportant to the actual behavior you are trying to assert, then Mockpico can make the setup easier.

Here is a simple example. Say FooController has a dozen dependencies injected in through the constructor, setters and appropriately annotated fields, but you just don’t care about most of the deps, the Mockpico can automatically inject mocks for them:

> FooController fc = mockDepsFor(FooController.class).withInjectees(b, c, d).make();

The setter methods or fields to inject into were annotated with Spring’s @Autowired or @Inject from JSR330. The three variables in this case are injected into where there dependent types are declared for FooController. Each of the three could be Mocks made by Mockito, or real or stub implementations as you see fit.

Here is a full test case showing Mockpico in use :- 
[MockpicoTestCase.java](blob/master/src/test/java/com/thoughtworks/mockpico/MockpicoTestCase.java).

However, there’s a word of warning :-

![alt injection diagram](raw/master/src/graffle/injection-diag.png "Collaborators Are Better")