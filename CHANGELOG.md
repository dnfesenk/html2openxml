# Changelog

## 2.1.0 — Unreleased

### Added

- Basic inline `color`, `font-size` (`px`/`pt`), `font-weight` (`normal`, `bold`, 100–900),
  `font-style` (`normal`, `italic`, `oblique`) and paragraph `text-align`.
- Inherited text styles survive cell fragment conversion, including nested tables. Inner styles
  override outer styles and restore correctly after closing tags. CSS on an element takes precedence
  over that element's legacy font attributes and bold/italic tags; `normal` explicitly clears emphasis.
- Unsupported CSS values are ignored in either validation mode. Stylesheet selectors, `!important`,
  relative font sizes/weights and general browser layout remain unsupported.

### Changed

- Default converters, including the existing map-only constructor, now ignore unknown paragraph
  `align` and cell `merge` values. Text, other formatting and remaining document content are retained.
- Add `HtmlToOpenXMLConverter(boolean strictValidation)` and
  `HtmlToOpenXMLConverter(Map<String, TagHandler>, boolean strictValidation)` to opt into the
  2.0.x behavior: unknown alignment and merge values raise `IllegalArgumentException`.
- The chosen mode applies to built-in paragraph and table handlers, including nested table cells.
  Standalone handler constructors retain their historical strict behavior; boolean overloads allow
  explicitly configuring replacement handlers.

### Compatibility notes

- **Behavior change:** callers relying on exceptions for unknown `align` or `merge` values must
  use `new HtmlToOpenXMLConverter(true)` or the map-plus-boolean constructor with `true`.
- This is limited attribute validation, not a full HTML/CSS validator. Image checks and resource
  limits still apply; custom-handler and document-operation failures are not swallowed.
- 2.1.0 is not published. Installation examples and the public demo remain on 2.0.1.

## 2.0.1 — 2026-11-18

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

When upgrading the demo from 2.0.0 to this release, deploy these changes together:

- Change the demo dependency, displayed version and version-specific documentation to 2.0.1.
- Remove “Use lowercase” from the `align` and `merge` validation messages.
- Normalize surrounding whitespace and case when previewing paragraph `align`.
- Replace the preview's legacy border substring check with declaration parsing matching the
  library: case-insensitive property/value, last declaration wins, only `none` removes borders.
- Verify mixed-case alignment, `border:none`, conflicting border declarations and all bundled
  examples against the generated DOCX.
