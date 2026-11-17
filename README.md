# html2openxml: Java HTML to OpenXML converter

**html2openxml** is a Java library that converts HTML content to OpenXML format (Microsoft Word `.docx` format), supporting common HTML elements like paragraphs, bold, italic, underline, tables, and more. Built using [docx4j](https://www.docx4java.org/) and [Jsoup](https://jsoup.org/), you can try a live demo of this library at https://html2openxml-demo.herokuapp.com/.

[![Maven Central](https://img.shields.io/maven-central/v/com.denisfesenko/html2openxml)](https://central.sonatype.com/artifact/com.denisfesenko/html2openxml)
[![CI](https://github.com/dnfesenk/html2openxml/actions/workflows/ci.yml/badge.svg)](https://github.com/dnfesenk/html2openxml/actions/workflows/ci.yml)

## Table of Contents
- [Installation](#installation)
- [Continuous Integration](#continuous-integration)
- [Usage](#usage)
- [Inline CSS](#inline-css)
- [Attribute Validation](#attribute-validation)
- [Supported HTML Elements](#supported-html-elements)
- [Images](#images)
- [Custom Tag Handlers](#custom-tag-handlers)
- [Publishing](#publishing)
- [Limitations](#limitations)
- [License](#license)

## Installation

Requires **Java 11 or later at runtime**. Building and running tests requires **JDK 17 or later**.
The library targets Java 11 bytecode; JUnit 6 tests target Java 17. Newer JDKs, including Java 25, are supported.

Build and test with Maven:

```sh
mvn clean test
```

Add the following dependency to your `pom.xml`:
```xml
<dependency>
  <groupId>com.denisfesenko</groupId>
  <artifactId>html2openxml</artifactId>
  <version>2.0.1</version>
</dependency>
```
Or if you're using Gradle, add this to your `build.gradle`:
```groovy
implementation 'com.denisfesenko:html2openxml:2.0.1'
```

Version 2.0.1 is published in Maven Central. See [CHANGELOG.md](CHANGELOG.md) for fixes
and compatibility notes.

## Continuous Integration

[GitHub Actions](https://github.com/dnfesenk/html2openxml/actions/workflows/ci.yml) runs on pull
requests, pushes to `main`, release tags and manual dispatch. Every matrix entry runs
`mvn clean verify`, builds the runtime dependency classpath, then compiles the actual README Usage
example with `javac --release 11`, runs it against the packaged JAR and checks its DOCX output.
The smoke check also verifies every library class has a bytecode major version no greater than 55.

| Runtime smoke check | Build and full JUnit suite |
| --- | --- |
| Java 11 | JDK 17 |
| Java 17 | JDK 17 |
| Java 25 | JDK 25 |

[JUnit 6 requires Java 17+](https://junit.org/), so the Java 11 matrix entry builds and runs the
full test suite on JDK 17 before switching to an actual JDK 11 for compilation and execution of
the README example. No JUnit tests are skipped. CI uses no release profile or publication secrets.

Run the same smoke check locally with the desired runtime in `JAVA_HOME`:

```sh
mvn --batch-mode --no-transfer-progress clean verify \
  org.apache.maven.plugins:maven-dependency-plugin:3.7.0:build-classpath \
  -DincludeScope=runtime -Dmdep.outputFile=target/runtime-classpath.txt
python3 scripts/readme_smoke.py
```

The Maven command requires JDK 17+; for the Java 11 runtime check, switch `JAVA_HOME` to JDK 11
only before running the Python script. Python 3 and a full JDK (including `javac`) are required.

## Usage
Here's a simple example of how to use **html2openxml** to convert HTML to a `.docx` file:
```java
import com.denisfesenko.converter.HtmlToOpenXMLConverter;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;

import java.io.File;

public class Main {
    public static void main(String[] args) {
        String htmlContent = "<html><body><p>Hello, world!</p></body></html>";
        try {
            HtmlToOpenXMLConverter converter = new HtmlToOpenXMLConverter();
            WordprocessingMLPackage wordDocument = converter.convert(htmlContent);

            File outputFile = new File("output.docx");
            wordDocument.save(outputFile);
        } catch (Docx4JException e) {
            e.printStackTrace();
        }
    }
}
```

## Inline CSS

**Since 2.1.0 (currently in development)**, inline text styles can be used directly, including
markup commonly produced by rich text editors:

```html
<p style="text-align:center;color:rebeccapurple;font-size:12pt">
  Regular <span style="font-weight:700;font-style:italic">emphasis</span>
</p>
```

| Property | Supported values |
| --- | --- |
| `color` | `#RGB`, `#RRGGBB`, `rgb(r,g,b)` and the 148 CSS named colors |
| `font-size` | Positive decimal `px` or `pt` values; 16px = 12pt |
| `font-weight` | `normal`, `bold`, and 100–900 in steps of 100; 600 and above become bold |
| `font-style` | `normal`, `italic`, `oblique` (rendered as italic) |
| `text-align` | `left`, `right`, `center`, `justify` on paragraphs |

Text properties inherit from ancestors, including table and cell styles. An inner declaration
can override an outer one; `normal` clears inherited bold or italic. Semantic tags and CSS are
applied from outer elements to inner elements, with CSS taking priority over the same element's
legacy attributes or semantic formatting. For example, `<b style="font-weight:normal">` is not bold,
while a nested `<b>` inside an element with `font-weight:normal` is bold. Paragraph alignment also
inherits from styled block ancestors; an explicit paragraph `align` overrides inherited alignment,
and a valid `text-align` on that paragraph overrides `align`.

Declarations accept property/value case and surrounding whitespace, and do not require a trailing
semicolon; the last declaration of each property is used. Unsupported values are ignored, preserving
inherited values, in both validation modes. This is a limited parser: external stylesheets, selectors,
`!important`, relative font sizes, font-size keywords and relative weights (`bolder`/`lighter`) are
not supported. Pixel/point sizes are rounded to Word half-points. In 2.0.x use `<font color size>`,
`<b>`, `<i>` and `<p align>` instead; the published demo remains on 2.0.1 until the 2.1.0 release.

## Attribute Validation

**Since 2.1.0 (currently in development)**, the default converter ignores unknown paragraph
`align` and cell `merge` values while preserving their text and other supported formatting:

```java
var converter = new HtmlToOpenXMLConverter(); // lenient by default
var document = converter.convert("<p align='middle'>Text is preserved</p>");
```

To retain the validation behavior of 2.0.x, enable strict validation explicitly:

```java
var converter = new HtmlToOpenXMLConverter(true); // strictValidation
// Unknown align or merge values raise IllegalArgumentException.
```

Custom handlers can be combined with either mode using
`new HtmlToOpenXMLConverter(customHandlers, true)` (strict) or
`new HtmlToOpenXMLConverter(customHandlers, false)` (lenient). The existing map-only constructor
is lenient in 2.1.0. The mode applies to built-in paragraph and table handlers, including conversions
inside nested table cells. Custom handlers retain their own validation behavior and their exceptions
are propagated. Standalone `ParagraphHandler()` and `TableHandler()` retain strict behavior;
use their boolean constructors to configure replacements explicitly.

This option only controls unknown `align` and `merge` values. Image validation, resource limits,
custom-handler failures and document-writing errors remain enforced in both modes. Strict mode
does not validate arbitrary HTML or all CSS properties.

## Supported HTML Elements
**html2openxml** supports the following HTML elements:
- `<p>` - Paragraph; `align="left|right|center|justify"` and `style="text-indent:10px"`
- `<b>`, `<strong>` - Bold
- `<i>`, `<em>` - Italic
- `<u>` - Underline
- `<sub>` - Subscript
- `<sup>` - Superscript
- `<table>` - Table; percentage `width` and `style="border:none"` for no borders
- `<tr>` - Table Row
- `<td>`, `<th>` - Table cells; `th` does not automatically add bold or centered text
- `<font>` - Text color and size; `size` is measured in pixels, with an optional `px` suffix
- `<span>` - Background colors and, since 2.1.0, inline text styles
- `<hr>` - Horizontal rule
- `<pb>` - Page Break
- `<br>` - Line Break
- `<img>` - Embedded PNG, JPEG or GIF from a Base64 data URI

Inline table/cell `width` percentages and paragraph `text-indent` pixels are parsed independently of
whitespace, property/unit case and a trailing semicolon; the last declaration wins.

Paragraph `align` accepts `left`, `right`, `center` and `justify`. Cells accept the custom
attribute `merge="restart|continue"` for vertical merging. In 2.0.x, unknown values raise
`IllegalArgumentException`. Since 2.1.0, they are ignored by default and rejected only in
[strict mode](#attribute-validation).
Since 2.0.1, these values accept surrounding whitespace and any letter case.
In 2.0.0 they are case-sensitive.

In 2.0.0, borderless tables require the exact substring `border: none;` (case-insensitive).
Since 2.0.1, `border` is parsed like other inline properties, so `border:none`
works without spaces or a trailing semicolon, and the last declaration wins.
This changes conflicting declarations: `border: none; border: 1px solid` is borderless in 2.0.0
but has borders in 2.0.1. See the [2.0.1 release notes](CHANGELOG.md).
Only the value `none` is recognized; `border:0` and `border:none !important` are not supported.
Use a standalone `border:none` declaration (or `border: none;` for 2.0.0).

`<font size="16">` and `<font size="16px">` both mean 16 pixels (12 points), **not** the legacy
HTML font-size scale of 1–7. Use explicit `px` for clarity. In 2.0.x, inline CSS `color`,
`font-size` and `text-align` are not supported; since 2.1.0 see [Inline CSS](#inline-css).

Only first-row cell widths define the table column grid. Place percentage widths on that row;
widths on later rows do not redefine the grid.

Table `colspan` values above 1000 are clamped to 1000; missing, zero, negative or malformed values use one column.

Colors in `<font color>`, `<span style="background-color: ...">` and table cells support
`#RGB`, `#RRGGBB`, `rgb(r, g, b)` and all 148 CSS named colors (case-insensitive).
Unsupported colors are ignored, preserving inherited formatting.

## Images
Since 2.0.0, the library supports inline images from Base64 data URIs, including images in table cells:

```html
<p>Before <img src="data:image/png;base64,..." width="200" alt="Chart"> after</p>
```

PNG, JPEG (`image/jpeg` or `image/jpg`) and GIF are supported. Base64 whitespace is allowed.
The `width` and `height` attributes accept pixels, including decimal values and an optional `px` suffix.
When one dimension is omitted, the aspect ratio is preserved. Without dimensions, the original pixel size
is used at 96 DPI. Images wider than the writable page area are scaled down proportionally.
The `alt` attribute is stored as the image description.

Remote URLs, local file paths and other image formats are not loaded; their `alt` text is inserted instead.
Malformed Base64, unrecognized image bytes or a MIME type that disagrees with the image data cause
`IllegalArgumentException`. Failures while embedding a valid image retain their runtime exception,
or are wrapped in `IllegalStateException` for checked docx4j exceptions.

Embedded images are limited to **10 MiB of decoded bytes per image** and **50 MiB of embedded
image data per document** by default. The document budget counts existing image parts, including
images in table cells and images added by earlier conversions or loaded from DOCX. It counts stored
binary data, not decoded pixels. Each new image is checked against the budget before Base64 decoding;
invalid image data does not consume budget. Failed document operations can leave image parts in the
package, and these parts still count toward the limit. This limit does not make conversion transactional.

For supported Base64 image data URIs, both URI length (including whitespace) and decoded size are
checked before decoding. The URI character limit is twice the padded Base64 length of the per-image
byte limit, plus 128 characters for the header. Unsupported sources, including long URLs and paths,
continue to use `alt` text.

Override either budget by registering an image handler, for example 5 MiB per image and 20 MiB per document:

```java
var converter = new HtmlToOpenXMLConverter(Map.of("img", new ImageHandler(5 * 1024 * 1024, 20L * 1024 * 1024)));
```

`ImageHandler` is in `com.denisfesenko.handler`; both limits must be positive. The one-argument
constructor changes only the per-image limit and keeps the default document budget.
Oversized inputs raise `IllegalArgumentException`.

## Custom Tag Handlers
You can extend the functionality of **html2openxml** by implementing your own custom tag handlers. Simply implement the `TagHandler` interface and register your handler with the `HtmlToOpenXMLConverter`:
```java
TagHandler customTagHandler = new TagHandler() {
    @Override
    public void handleTag(Node node, WordprocessingMLPackage wordMLPackage) {
        //Custom implementation
    }

    @Override
    public boolean isRepeatable() {
        return true;
    }
};

HtmlToOpenXMLConverter converter = new HtmlToOpenXMLConverter(Map.of("custom-tag", customTagHandler));
```
Legacy repeatable handlers run once per text node, including when the same handler is registered for nested tags.
Handlers that need inherited properties can override `handleTag(Node, List<Node>, WordprocessingMLPackage)`;
the source elements are provided from outermost to innermost.

Now, when the converter encounters an element with the tag name `<custom-tag>`, it will call the `handleTag` method of your `customTagHandler` instance.

## Publishing

Releases use the [Sonatype Central Portal Maven plugin](https://central.sonatype.org/publish/publish-portal-maven/)
through the `release` profile. Configure your Portal token in Maven `settings.xml` under server ID `central`
and a GPG signing key locally. Credentials are not stored in this repository.

```xml
<server>
  <id>central</id>
  <username>${env.CENTRAL_TOKEN_USERNAME}</username>
  <password>${env.CENTRAL_TOKEN_PASSWORD}</password>
</server>
```

Build a local bundle without signing or uploading:

```sh
mvn -Prelease deploy -Dcentral.skipUpload=true -Dgpg.skip=true
```

For a signed deployment, run `mvn -Prelease deploy`. This uploads for validation;
`autoPublish` is disabled, so publication must be completed in the Central Portal.
Normal `mvn verify` builds require neither publishing credentials nor a GPG key.

## Limitations
This library is designed to handle a subset of HTML elements and does not provide support for all HTML5 tags and attributes. It supports the inline style properties described above, rather than a complete CSS layout engine. If you need more advanced conversion features, you may need to consider other options or extend this library with custom tag handlers.

## License
**html2openxml** is released under the MIT License. See the LICENSE file for more details.