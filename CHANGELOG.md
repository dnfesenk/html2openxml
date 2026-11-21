# Changelog

## Unreleased

### Added

- HTML links with absolute HTTP, HTTPS, or mailto addresses now produce clickable Word
  hyperlinks styled with the document's Hyperlink character style.
- HTML headings now use Word heading styles, which affect formatting and the navigation outline.
  Existing template heading styles are reused, including styles with localized IDs.
- HTML lists now produce native Word numbering. List item paragraphs receive `numPr` and the
  DOCX gains a numbering part. Continuation paragraphs align with the item text and have no
  additional marker. Custom list handlers retain control of the tags they override.

### Fixed

- Converters can share a `TableHandler` without overwriting the converter used for table cells.

### Compatibility notes

- `HtmlToOpenXMLConverter` now dispatches to the three-argument `TableHandler.handleTag`
  overload. Subclasses registered as custom table handlers must override that overload
  for their customization to run during conversion. Direct calls to the two-argument
  overload still use the converter configured with `addConverter`.

## 2.1.0 — 2026-11-18

### Added

- GitHub Actions CI for Java 11/17/25 runtime coverage. Every matrix entry runs Maven verify;
  the Java 11 entry builds on JDK 17 with `-DskipTests`, then uses real JDK 11 for the README smoke.
  Full JUnit suites run on JDK 17 and 25 without a duplicate run in the Java 11 job.
  Smoke checks compile the actual Usage example with `--release 11`, run the packaged library,
  validate the DOCX output and check the library's Java 11 bytecode. Failed jobs retain test reports.

- Basic inline `color`, `font-size` (`px`/`pt`), `font-weight` (`normal`, `bold`, 100–900),
  `font-style` (`normal`, `italic`, `oblique`) and paragraph `text-align`, including implicit
  paragraphs for bare text in blocks and table cells.
- Inherited text styles survive cell fragment conversion, including nested tables. Inner styles
  override outer styles and restore correctly after closing tags. CSS on an element takes precedence
  over that element's legacy font attributes and bold/italic tags; `normal` explicitly clears emphasis.
- Unsupported CSS values are ignored in either validation mode. Stylesheet selectors, `!important`,
  relative font sizes/weights and general browser layout remain unsupported.

### Changed

- Unsupported image sources without alt text no longer leave speculative empty paragraphs or
  apply their block alignment to existing content. Successful images and nonempty alt fallbacks
  retain block boundaries and inherited alignment.

- Inherited cell-style wrappers use the reserved `html2openxml-inherited-style` tag, so adding
  CSS to a table does not invoke a user's `div` handler. Registering the reserved tag is rejected.
- Initialize implicit paragraph alignment even after content has been added, without replacing
  an existing justification. Images and line breaks also initialize inherited alignment, including
  image-only cells, image-before-text cells and leading breaks.

- `TagHandlerFactory.createTagHandlerMap()` now returns lenient built-in handlers by default;
  use `createTagHandlerMap(true)` for strict alignment/merge validation.
- Cap CSS and legacy font sizes at 3276 half-points (1638pt).
- Separate implicit paragraphs at block boundaries and inherit the nearest block alignment,
  preventing alignment from leaking across adjacent or nested blocks.

- Default converters, including the existing map-only constructor, now ignore unknown paragraph
  `align` and cell `merge` values. Text, other formatting and remaining document content are retained.
- Add `HtmlToOpenXMLConverter(boolean strictValidation)` and
  `HtmlToOpenXMLConverter(Map<String, TagHandler>, boolean strictValidation)` to opt into the
  2.0.x behavior: unknown alignment and merge values raise `IllegalArgumentException`.
- The chosen mode applies to built-in paragraph and table handlers, including nested table cells.
  Standalone handler constructors retain their historical strict behavior; boolean overloads allow
  explicitly configuring replacement handlers.

### Compatibility notes

- **Block layout changes even without CSS:** `<div>a</div><div>b</div>` produced one paragraph
  in 2.0.x and produces two paragraphs in 2.1. The same block-boundary handling applies to `li`.
  Inline content such as `x <b>y</b> z` remains in one paragraph.

- **Behavior change:** callers relying on exceptions for unknown `align` or `merge` values must
  use `new HtmlToOpenXMLConverter(true)` or the map-plus-boolean constructor with `true`.
- This is limited attribute validation, not a full HTML/CSS validator. Image checks and resource
  limits still apply; custom-handler and document-operation failures are not swallowed.
- 2.1.0 is published in Maven Central. Upgrade the demo dependency and preview together.

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
