package pdfpp

import com.itextpdf.text._
import java.io._
import pdf._

object PdfPPMain {

  def main(args: Array[String]) = {
    println(try {
      args match {
        case Array("stamp", message, in, out) => stampPdf(in, out, message); "Done"
        case Array("rotate", degrees, in, out) => rotatePdf(in, out, degrees.toInt); "Done"
        case Array("crop", margins, in, out) => {
          cropPdf(in, out, (p:Int, pageRect:Rectangle, cropRect:Option[Rectangle]) =>
            parseMargins(margins).shrink(if (cropRect.isDefined) cropRect.get else pageRect))
          "Done"
        }
        case Array("vsplit", intoPages, in, out) => vsplitPages(in, out, intoPages.toInt); "Done"
        case _ =>
          """
Usage: pdfpp <command> [command-options] <input.pdf> <output.pdf>
List of supported commands with options:
 vsplit <n>
    Vertically split every page into n parts.
 rotate <degrees>
    Rotate every page by the specified angle counterclockwise.
 crop <left,top,right,bottom>
    Crop every page by the given margins
 stamp <message>
    Add the specified message at the top of every page.
          """
      }
    } catch {
      case (e:IOException) => "Error: " + e.getMessage
    })
  }

  def parse4Floats(str:String):(Float, Float, Float, Float) = {
    val FourFloats= "(-?[\\d\\.]+),(-?[\\d\\.]+),(-?[\\d\\.]+),(-?[\\d\\.]+)".r
    str match {
      case FourFloats(a,b,c,d) => (a.toFloat, b.toFloat, c.toFloat, d.toFloat)
      case _ => throw new IllegalArgumentException("Cannot parse parameter values: " + str)
    }
  }

  def parseRect(coords:String):Rectangle = {
    val parsed = parse4Floats(coords)
    new Rectangle(parsed._1, parsed._4, parsed._3, parsed._2)
  }

  def parseMargins(margins:String):Margins = {
    val parsed = parse4Floats(margins)
    new Margins(parsed._1, parsed._2, parsed._3, parsed._4)
  }

  type GetCropRect = (Int, Rectangle, Option[Rectangle])  => Rectangle

  def cropPdf(src:String, dest:String, getCropRect:GetCropRect) {
    val reader = new PdfReader(src)
    printf("Cropping %d pages\n", reader.getNumberOfPages)
    for (i <- 1 to reader.getNumberOfPages) {
      val page = reader.getPageN(i)
      val crop = page.get(PdfName.CROPBOX) match {
        case a:PdfArray => Some(new Rectangle(
            a.getAsNumber(0).intValue, a.getAsNumber(1).intValue,
            a.getAsNumber(2).intValue, a.getAsNumber(3).intValue))
        case _ => None
      }
      page.put(PdfName.CROPBOX, new PdfRectangle(getCropRect(i, reader.getPageSize(page), crop)))
    }
    val stamper = new PdfStamper(reader, new FileOutputStream(dest), PdfWriter.VERSION_1_5)
    printf("Saving to file '%s'\n", dest)
    stamper.close
  }

  def rotatePdf(src:String, dest:String, degrees:Int) {
    val reader = new PdfReader(src)
    printf("Rotating %d pages\n", reader.getNumberOfPages)
    for (i <- 1 to reader.getNumberOfPages) {
      val page = reader.getPageN(i)
      val rotation = page.get(PdfName.ROTATE) match {
        case a:PdfArray => a.getAsNumber(0).intValue
        case _ => 0
      }
      page.put(PdfName.ROTATE, new PdfNumber((rotation + degrees) % 360))
    }
    val stamper = new PdfStamper(reader, new FileOutputStream(dest), PdfWriter.VERSION_1_5)
    printf("Saving to file '%s'\n", dest)
    stamper.close
  }

  case class Margins(left:Float, top:Float, right:Float, bottom:Float) {
    def shrink(rect:Rectangle) = new Rectangle(rect.getLeft + left, rect.getBottom + bottom, rect.getRight - right, rect.getTop - top)
  }

  def copyDocument(src:String, dest:String, repeatEveryPageTimes:Int) {
    val reader = new PdfReader(src)
    val document = new Document()

    val copy = new PdfCopy(document, new FileOutputStream(dest))
    document.open()
    for (i <- 1 to reader.getNumberOfPages) {
      val p = copy.getImportedPage(reader, i)
      for (k <- 1 to repeatEveryPageTimes) {
        copy.addPage(p)
      }
    }
    document.close
    copy.close
  }

  def vsplitPages(src:String, dest:String, intoNumPages:Int) {
    val tempFile = File.createTempFile("pdfpp", "._")
    copyDocument(src, tempFile.getPath, intoNumPages)

    cropPdf(tempFile.getPath, dest, (page:Int, pageRect:Rectangle, crop:Option[Rectangle]) => {
      val pr = if (crop.isDefined) crop.get else pageRect
      val (left, bottom, right, top) = (pr.getLeft, pr.getBottom, pr.getRight, pr.getTop)
      val segh = (top - bottom)/intoNumPages
      val i = ((page - 1) % intoNumPages)
      new Rectangle(left, top - segh * (i + 1), right, top - segh * i)
    })
    if (!tempFile.delete) tempFile.deleteOnExit
  }

  def stampPdf(src:String, dest:String, message:String) {
    printf("Adding stamp to file '%s'\n", src)
    printf("Message: '%s'\n", message)
    val reader = new PdfReader(src)
    val stamper = new PdfStamper(reader, new FileOutputStream(dest))

    printf("Adding a stamp on %d pages\n", reader.getNumberOfPages)
    for (i <- 1 to reader.getNumberOfPages) {
      val cb = stamper.getUnderContent(i)
      cb.setFontAndSize(BaseFont.createFont(BaseFont.HELVETICA, "Cp1252", true), 10)
      cb.setColorFill(new BaseColor(150, 150, 150))
      cb.beginText
      val doc = cb.getPdfDocument
      val top = doc.top
      val right = doc.right
      cb.showTextAligned(PdfContentByte.ALIGN_CENTER, message,
        doc.leftMargin + right/2, doc.topMargin + top - doc.topMargin/2 , 0)
      cb.endText
    }
    printf("Saving to file '%s'\n", dest)
    stamper.close
  }
   
}
