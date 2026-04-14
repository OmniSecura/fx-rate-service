# FX Rate Service Tests

This document describes the test suite for the FX Rate Service, including what each test covers and how to run them.

## Test Overview

### Unit Tests

1. **Cross Rate Calculation**
   - Tests the calculation of cross rates (e.g., EURUSD derived from EURGBP and GBPUSD).
   - Verifies correct calculation with known values.
   - Ensures exceptions are thrown when a required rate leg is missing.

2. **Staleness Detection**
   - Tests that exchange rates older than 4 hours are flagged as stale.
   - Includes boundary testing for rates exactly at the 4-hour threshold.

3. **Currency Conversion**
   - Tests accurate currency conversion (e.g., 10,000 GBP to USD at rate 1.2650 should equal 12,650.00 USD).

### Mock Tests

4. **Rate Persistence**
   - Mocks the repository to verify that correct parameters are passed when storing bid/ask/mid rates.

5. **Rate Retrieval**
   - Mocks the repository to test fallback behavior when no rate is found for a currency pair.

### Parameterized Tests

6. **Edge Cases**
   - Tests various edge cases including zero amount conversion, unknown currency pairs, and other boundary conditions.

## How to Run the Tests

### Prerequisites
- Java 21
- Maven 3.x
- Spring Boot Test dependencies (included in pom.xml)

### Running Tests with Maven
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=RateServiceTest

# Run with verbose output
mvn test -Dtest=RateServiceTest -DforkCount=1 -DreuseForks=false

# Generate test report
mvn surefire-report:report
```

### Running Tests in IDE
- **IntelliJ IDEA**: Right-click on `RateServiceTest.java` and select "Run Tests"
- **Eclipse**: Right-click on the test class and select "Run As > JUnit Test"
- **VS Code**: Use the Java Test Runner extension

### Test Configuration
- Tests use Mockito for mocking dependencies
- Spring Boot Test framework is utilized for integration
- Parameterized tests use JUnit 5's `@ParameterizedTest`

### Coverage
To check test coverage, you can use JaCoCo:
```bash
mvn jacoco:prepare-agent test jacoco:report
```
Reports will be generated in `target/site/jacoco/`

## Test Structure
- `src/test/java/com/FXplore/fx_rate_service/RateServiceTest.java` - Main test class
- Uses JUnit 5, Mockito, and Spring Boot Test annotations
- Tests are organized by type (Unit, Mock, Parameterized) with clear naming conventions
