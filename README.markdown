pdfpp
=====
pdfpp is a command-line tool for pdf postprocessing.

Usage: java -jar pdfpp.jar <command> [command-options] <input.pdf> <output.pdf>

List of supported commands with options:

_vsplit &lt;n&gt;_

  Vertically split every page into n parts.

_rotate &lt;degrees&gt;_

  Rotate every page by the specified angle counterclockwise.

_crop &lt;left,top,right,bottom&gt;_

  Crop every page by the given margins

_stamp &lt;message&gt;_

  Add the specified message at the top of every page.


This tool is intented mainly for improving the PDF reading experience on e-book readers (e.g. Sony Reader, or Kindle). The screen of a reader is not large enough to fit a page of a not-especially prepared PDF document. Zooming often requres "reflowing" the document content. Such reflowing is destructive for tables or any special formatting which especially makes reading of computer-related books very unpleasant.

The following sequence of pdfpp commands removes the margins, splits every page in two parts vertically, and rotates them, so that each half page takes the whole screen of the reader and thus enlarges the view without the need for reflowing.

    crop 82,25,85,25
    vsplit 2
    rotate -90

