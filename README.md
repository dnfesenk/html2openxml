# html2openxml: Java HTML to OpenXML converter

**html2openxml** is a Java library that converts HTML content to OpenXML format (Microsoft Word `.docx` format), supporting common HTML elements like paragraphs, bold, italic, underline, tables, and more. Built using [docx4j](https://www.docx4java.org/) and [Jsoup](https://jsoup.org/), you can try a live demo of this library at https://html2openxml-demo.herokuapp.com/.

[![Maven Central](https://img.shields.io/maven-central/v/com.denisfesenko/html2openxml)](https://central.sonatype.com/artifact/com.denisfesenko/html2openxml)
## Table of Contents
- [Installation](#installation)
- [Usage](#usage)
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
  <version>2.0.0</version>
</dependency>
```
Or if you're using Gradle, add this to your `build.gradle`:
```groovy
implementation 'com.denisfesenko:html2openxml:2.0.0'
```

## Usage
Here's a simple example of how to use **html2openxml** to convert HTML to a `.docx` file:
```java
import com.denisfesenko.converter.HtmlToOpenXMLConverter;
import org.docx4j.openpackaging.exceptions.InvalidFormatException;
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
        } catch (InvalidFormatException e) {
            e.printStackTrace();
        }
    }
}
```

## Supported HTML Elements
**html2openxml** supports the following HTML elements:
- `<p>` - Paragraph
- `<b>`, `<strong>` - Bold
- `<i>`, `<em>` - Italic
- `<u>` - Underline
- `<sub>` - Subscript
- `<sup>` - Superscript
- `<table>` - Table
- `<tr>` - Table Row
- `<td>` - Table Cell
- `<span>` - Specifically dealing with background colors
- `<hr>` - Horizontal rule
- `<pb>` - Page Break
- `<br>` - Line Break
- `<img>` - Embedded PNG, JPEG or GIF from a Base64 data URI

Inline table/cell `width` percentages and paragraph `text-indent` pixels are parsed independently of
whitespace, property/unit case and a trailing semicolon; the last declaration wins.

Table `colspan` values above 1000 are clamped to 1000; missing, zero, negative or malformed values use one column.

Colors in `<font color>`, `<span style="background-color: ...">` and table cells support
`#RGB`, `#RRGGBB`, `rgb(r, g, b)` and all 148 CSS named colors (case-insensitive).
Unsupported colors are ignored, preserving inherited formatting.

## Images
The current source version supports inline images from Base64 data URIs, including images in table cells:

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