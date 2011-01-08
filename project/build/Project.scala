import sbt._

class Project(info: ProjectInfo) extends DefaultProject(info) with ProguardProject {


  val itext = "com.itextpdf" % "itextpdf" % "5.0.4"
  val codeHouseRep = "Codehouse release repository" at "http://repository.code-house.org/content/repositories/release/"

  override def mainClass = Some("pdfpp.PdfPPMain")

  override def makeInJarFilter(file: String) = super.makeInJarFilter(file) + ",!META-INF/BCKEY.DSA,!META-INF/BCKEY.SF"


  override def allDependencyJars = (super.allDependencyJars +++
    Path.fromFile(buildScalaInstance.compilerJar) +++
    Path.fromFile(buildScalaInstance.libraryJar)
  )

  override def proguardOptions = List(
    proguardKeepMain("pdfpp.PdfPPMain"),
   "-keep interface scala.ScalaObject"
  )
}
