package predicate_calculus;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)

@Suite.SuiteClasses({
        ParserTest.class,
        ValidatorTest.class,
        DeductorTest.class
})

public class TestAll {
}
