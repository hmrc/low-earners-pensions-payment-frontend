package forms

import forms.mappings.Mappings
import play.api.data.Mapping
import play.api.data.validation.Constraint

trait BaseForm extends Mappings {
  private[forms] def stripWhitespace(str: String): String = str.strip().replaceAll(" {2,}", " ")

  private[forms] def stripOptionalWhitespace(strOpt: Option[String]) = strOpt.map(stripWhitespace)

  private type MappingFor[A] = (String, Mapping[A])

  def mandatoryTextField(fieldName: String,
                         errorKeyPrefix: String,
                         minAcceptedLength: Int,
                         maxAcceptedLength: Int,
                         regex: String,
                         bindMap: String => String = stripWhitespace,
                         unbindMap: String => String = identity[String]): MappingFor[String] =
    fieldName -> text(s"$errorKeyPrefix.formError.required.$fieldName")
      .transform(bindMap, unbindMap)
      .verifying(
        if (minAcceptedLength != maxAcceptedLength) {
          firstError(
            minLength(minAcceptedLength, s"$errorKeyPrefix.formError.length.$fieldName"),
            maxLength(maxAcceptedLength, s"$errorKeyPrefix.formError.length.$fieldName"),
            regexp(regex, s"$errorKeyPrefix.formError.format.$fieldName")
          )
        } else {
          firstError(
            exactLength(minAcceptedLength, s"$errorKeyPrefix.formError.length.$fieldName"),
            regexp(regex, s"$errorKeyPrefix.formError.format.$fieldName")
          )
        }
      )

  def optionalTextField(fieldName: String,
                        errorKeyPrefix: String,
                        minAcceptedLength: Int,
                        maxAcceptedLength: Int,
                        regex: String,
                        bindMap: Option[String] => Option[String] = stripOptionalWhitespace,
                        unbindMap: Option[String] => Option[String] = identity[Option[String]]): MappingFor[Option[String]] =
    fieldName -> textOpt()
      .transform(bindMap, unbindMap)
      .verifying(
        if (minAcceptedLength != maxAcceptedLength) {
          firstErrorOpt(
            minLength(minAcceptedLength, s"$errorKeyPrefix.formError.length.$fieldName"),
            maxLength(maxAcceptedLength, s"$errorKeyPrefix.formError.length.$fieldName"),
            regexp(regex, s"$errorKeyPrefix.formError.format.$fieldName")
          )
        } else {
          firstErrorOpt(
            exactLength(minAcceptedLength, s"$errorKeyPrefix.formError.length.$fieldName"),
            regexp(regex, s"$errorKeyPrefix.formError.format.$fieldName")
          )
        }
      )
}
