# IntelliJ Platform Swing Components

This module contains a set of Swing components that can be used in IntelliJ Platform plugins.

## Patched IntelliJ components

Some components have some rendering issues when the UI is scaled like in presentation mode.


* Patched `JBTable` and it's subtype `TableView` to respectively `ScalableJBTable` and `ScalableTableView`.
  See [IDEA-289745](https://youtrack.jetbrains.com/issue/IDEA-289745) for details.

