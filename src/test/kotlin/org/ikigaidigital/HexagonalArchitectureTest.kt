package org.ikigaidigital

import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.Test

class HexagonalArchitectureTest {

    private val productionClasses = ClassFileImporter()
        .withImportOption(ImportOption.DoNotIncludeTests())
        .importPackages("org.ikigaidigital")

    @Test
    fun `domain and legacy core classes must not depend on frameworks or outer layers`() {
        val isLegacyCoreClass = object : DescribedPredicate<JavaClass>("reside in legacy root package excluding Application") {
            override fun test(input: JavaClass): Boolean =
                input.packageName == "org.ikigaidigital" &&
                    !input.name.startsWith("org.ikigaidigital.Application")
        }

        val isDomainPackageClass = JavaClass.Predicates.resideInAnyPackage("org.ikigaidigital.domain..")

        noClasses()
            .that(isLegacyCoreClass.or(isDomainPackageClass))
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "org.springframework..",
                "jakarta..",
                "org.ikigaidigital.application..",
                "org.ikigaidigital.adapter..",
                "org.ikigaidigital.config.."
            )
            .check(productionClasses)
    }

    @Test
    fun `application layer must not depend on frameworks, adapters, or config`() {
        noClasses()
            .that()
            .resideInAnyPackage("org.ikigaidigital.application..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "org.springframework..",
                "jakarta..",
                "org.ikigaidigital.adapter..",
                "org.ikigaidigital.config.."
            )
            .check(productionClasses)
    }

    @Test
    fun `REST adapter must not depend on persistence adapter`() {
        noClasses()
            .that()
            .resideInAnyPackage("org.ikigaidigital.adapter.rest..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.ikigaidigital.adapter.persistence..")
            .check(productionClasses)
    }

    @Test
    fun `persistence adapter must not depend on REST adapter`() {
        noClasses()
            .that()
            .resideInAnyPackage("org.ikigaidigital.adapter.persistence..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.ikigaidigital.adapter.rest..")
            .check(productionClasses)
    }
}
