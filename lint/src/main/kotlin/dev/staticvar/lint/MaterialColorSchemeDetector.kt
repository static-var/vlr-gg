package dev.staticvar.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import org.jetbrains.uast.UQualifiedReferenceExpression

internal class MaterialColorSchemeDetector : Detector(), SourceCodeScanner {
  override fun getApplicableUastTypes(): List<Class<out org.jetbrains.uast.UElement>> =
    listOf(UQualifiedReferenceExpression::class.java)

  override fun createUastHandler(context: JavaContext): UElementHandler =
    object : UElementHandler() {
      override fun visitQualifiedReferenceExpression(node: UQualifiedReferenceExpression) {
        val reference = node.asSourceString()
        if (
          reference.startsWith(MATERIAL_THEME_COLOR_SCHEME) ||
            reference.startsWith(MATERIAL_THEME_COLORS)
        ) {
          context.report(
            ISSUE,
            node,
            context.getNameLocation(node),
            "Use MaterialTheme.prismColors instead of $reference.",
          )
        }
      }
    }

  companion object {
    private const val MATERIAL_THEME_COLOR_SCHEME = "MaterialTheme.colorScheme"
    private const val MATERIAL_THEME_COLORS = "MaterialTheme.colors"

    val ISSUE: Issue =
      Issue.create(
        id = "PrismColorUsage",
        briefDescription = "Prefer MaterialTheme.prismColors",
        explanation =
          "Prism exposes an extended color palette via MaterialTheme.prismColors. " +
            "Reference it instead of MaterialTheme.colorScheme or MaterialTheme.colors.",
        category = Category.CORRECTNESS,
        priority = 6,
        severity = Severity.WARNING,
        implementation =
          Implementation(
            MaterialColorSchemeDetector::class.java,
            Scope.JAVA_FILE_SCOPE,
          ),
      )
  }
}
