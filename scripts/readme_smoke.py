#!/usr/bin/env python3
"""Compile the actual README Usage example for Java 11 and execute the packaged library."""

import os
from pathlib import Path
import re
import struct
import subprocess
import tempfile
import xml.etree.ElementTree as ET
import zipfile


def main():
    root = Path(__file__).resolve().parent.parent
    readme = (root / "README.md").read_text(encoding="utf-8")
    usage = re.search(r"^## Usage\s*\n(.*?)(?=^## |\Z)", readme, re.MULTILINE | re.DOTALL)
    if usage is None:
        raise RuntimeError("README Usage section not found")
    example = re.search(r"^```java\s*\n(.*?)^```", usage.group(1), re.MULTILINE | re.DOTALL)
    if example is None:
        raise RuntimeError("README Usage Java example not found")

    ns = {"m": "http://maven.apache.org/POM/4.0.0"}
    pom = ET.parse(root / "pom.xml").getroot()
    artifact = pom.findtext("m:artifactId", namespaces=ns)
    version = pom.findtext("m:version", namespaces=ns)
    jar = root / "target" / f"{artifact}-{version}.jar"
    classpath_file = root / "target" / "runtime-classpath.txt"
    if not jar.is_file() or not classpath_file.is_file():
        raise RuntimeError("Build the JAR and target/runtime-classpath.txt before running this check")

    # Check every library class, not just the classes reached by the example.
    with zipfile.ZipFile(jar) as archive:
        classes = [name for name in archive.namelist() if name.endswith(".class")]
        if not classes:
            raise RuntimeError("Packaged library contains no classes")
        for name in classes:
            header = archive.read(name)[:8]
            magic, _, major = struct.unpack(">IHH", header)
            if magic != 0xCAFEBABE or major > 55:
                raise RuntimeError(f"{name} is not Java 11 compatible (major={major})")

    java_home = os.environ.get("JAVA_HOME")
    java = str(Path(java_home) / "bin" / "java") if java_home else "java"
    javac = str(Path(java_home) / "bin" / "javac") if java_home else "javac"
    classpath = os.pathsep.join((str(jar), classpath_file.read_text(encoding="utf-8").strip()))
    subprocess.run([java, "-version"], check=True)
    with tempfile.TemporaryDirectory(prefix="html2openxml-readme-") as work:
        work_dir = Path(work)
        source = work_dir / "Main.java"
        source.write_text(example.group(1), encoding="utf-8")
        subprocess.run([javac, "--release", "11", "-encoding", "UTF-8", "-cp", classpath,
                        "-d", work, str(source)], check=True)
        subprocess.run([java, "-Djava.awt.headless=true", "-cp", os.pathsep.join((work, classpath)),
                        "Main"], cwd=work, check=True)
        # The README catches Docx4JException, so exit code alone cannot detect a failed save.
        with zipfile.ZipFile(work_dir / "output.docx") as archive:
            document = ET.fromstring(archive.read("word/document.xml"))
        word_ns = {"w": "http://schemas.openxmlformats.org/wordprocessingml/2006/main"}
        text = "".join(node.text or "" for node in document.findall(".//w:t", word_ns))
        if text != "Hello, world!":
            raise RuntimeError(f"Unexpected DOCX text: {text!r}")
    print("PASS: library bytecode <= 55; README compiles with --release 11 and produces a readable DOCX")


if __name__ == "__main__":
    main()
