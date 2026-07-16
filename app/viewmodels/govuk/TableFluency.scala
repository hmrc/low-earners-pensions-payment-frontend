/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package viewmodels.govuk

import uk.gov.hmrc.govukfrontend.views.viewmodels.table.{HeadCell, Table, TableRow}

trait TableFluency {
  object TableViewModel {
    def apply(caption: String,
              rows: Seq[Seq[TableRow]],
              columnHeaders: Seq[HeadCell],
              tableRef: String,
              captionIsHidden: Boolean = true): Table = {
      val baseCaptionClasses: String = "govuk-table__caption--m"
      val captionClasses: String = if(captionIsHidden) s"$baseCaptionClasses govuk-visually-hidden" else baseCaptionClasses
      
      Table(
        rows = rows,
        head = Some(columnHeaders),
        caption = Some(caption),
        captionClasses = captionClasses,
        attributes = Map("id" -> s"$tableRef")
      )
    }
  }
}
