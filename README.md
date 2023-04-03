# html2openxml: Java HTML to OpenXML converter

**html2openxml** is a Java library that converts HTML content to OpenXML format (Microsoft Word `.docx` format), supporting common HTML elements like paragraphs, bold, italic, underline, tables, and more. Built using [docx4j](https://www.docx4java.org/) and [Jsoup](https://jsoup.org/), you can try a live demo of this library at https://html2openxml-demo.herokuapp.com/.

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.denisfesenko/html2openxml/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.denisfesenko/html2openxml)
## Table of Contents
- [Installation](#installation)
- [Usage](#usage)
- [Supported HTML Elements](#supported-html-elements)
- [Custom Tag Handlers](#custom-tag-handlers)
- [Limitations](#limitations)
- [License](#license)

## Installation
Add the following dependency to your `pom.xml`:
```xml
<dependency>
  <groupId>com.denisfesenko</groupId>
  <artifactId>html2openxml</artifactId>
  <version>1.0.0</version>
</dependency>
```
Or if you're using Gradle, add this to your `build.gradle`:
```groovy
implementation 'com.denisfesenko:html2openxml:1.0.0'
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
Now, when the converter encounters an element with the tag name `<custom-tag>`, it will call the `handleTag` method of your `customTagHandler` instance.

## Limitations
This library is designed to handle a subset of HTML elements and does not provide support for all HTML5 tags and attributes. It also does not handle CSS styles. If you need more advanced conversion features, you may need to consider other options or extend this library with custom tag handlers.

## License
**html2openxml** is released under the MIT License. See the LICENSE file for more details.