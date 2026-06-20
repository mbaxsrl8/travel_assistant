# Repository Instructions

## Interface Documentation

- Add Javadoc to every Java interface describing its responsibility.
- Add Javadoc to every interface method describing its behavior, parameters, return value, and declared exceptions where applicable.

## Service Development

- Follow test-driven development (TDD) for every service class: write a failing test first, implement the smallest change that makes it pass, and then refactor while keeping the suite green.
- Add or update unit tests for every service behavior and edge case in the same change as the production code.
- Mock external boundaries such as cache and persistence repositories in service unit tests so service behavior is verified independently.
