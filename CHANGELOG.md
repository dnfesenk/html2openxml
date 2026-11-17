# Changelog

## 2.0.1 — Unreleased

### Fixes

- Accept surrounding whitespace and mixed case in paragraph `align` and cell `merge` values.
  Unknown values still raise `IllegalArgumentException`.
- Parse table `border` declarations independently of whitespace, property/value case and a trailing
  semicolon. `border:none` now produces a borderless table.
- Correct the README Usage exception handling and document supported tags, attributes, pixel font
  sizes, first-row column widths and unsupported CSS properties.

### Compatibility notes

- **Conflicting table borders now use the last declaration.** In 2.0.0,
  `style="border: none; border: 1px solid"` produced a borderless table because any matching
  substring was sufficient. In 2.0.1 it produces a table with borders. To keep a table borderless,
  remove the conflicting declaration or put `border:none` last.
- `border:0` and `border:none !important` remain unsupported. This is a limited inline-style
  parser, not a complete CSS cascade implementation.
- Deprecate `Constants.TABLE_BORDERLESS_STYLE`; retain it for source and binary compatibility.

### Demo upgrade checklist

Deploy these changes together only after 2.0.1 is available from Maven Central:

- Change the demo dependency, displayed version and version-specific documentation to 2.0.1.
- Remove “Use lowercase” from the `align` and `merge` validation messages.
- Normalize surrounding whitespace and case when previewing paragraph `align`.
- Replace the preview's legacy border substring check with declaration parsing matching the
  library: case-insensitive property/value, last declaration wins, only `none` removes borders.
- Verify mixed-case alignment, `border:none`, conflicting border declarations and all bundled
  examples against the generated DOCX.
