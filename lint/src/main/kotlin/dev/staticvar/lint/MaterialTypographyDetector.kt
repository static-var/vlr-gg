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

internal class MaterialTypographyDetector : Detector(), SourceCodeScanner {
  override fun getApplicableUastTypes(): List<Class<out org.jetbrains.uast.UElement>> =
    listOf(UQualifiedReferenceExpression::class.java)

  override fun createUastHandler(context: JavaContext): UElementHandler =
    object : UElementHandler() {
      override fun visitQualifiedReferenceExpression(node: UQualifiedReferenceExpression) {
        val reference = node.asSourceString()
        if (reference.startsWith(MATERIAL_THEME_TYPOGRAPHY_REFERENCE)) {
          context.report(
            ISSUE,
            node,
            context.getNameLocation(node),
            "Use MaterialTheme.prismTypography instead of MaterialTheme.typography.",
          )
        }
      }
    }

  companion object {
    private const val MATERIAL_THEME_TYPOGRAPHY_REFERENCE = "MaterialTheme.typography"

    val ISSUE: Issue =
      Issue.create(
        id = "PrismTypographyUsage",
        briefDescription = "Prefer MaterialTheme.prismTypography",
        explanation =
          "Prism extends Material typography with semantic tokens. Use " +
            "MaterialTheme.prismTypography to ensure consistent styling.",
        category = Category.CORRECTNESS,
        priority = 6,
        severity = Severity.WARNING,
        implementation =
          Implementation(
            MaterialTypographyDetector::class.java,
            Scope.JAVA_FILE_SCOPE,
          ),
      )
  }
}
