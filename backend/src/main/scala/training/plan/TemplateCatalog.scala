package training.plan

import training.plan.templates.*

/** Registry of available training plan templates. To add a new template,
  * implement `PlanTemplate` in `training.plan.templates` and add it to `all`.
  */
object TemplateCatalog:

  val all: List[PlanTemplate] = List(
    LeadvilleMarathon,
    HalHigdonNovice1Marathon,
    HansonBeginnerMarathon
  )

  private val byKey: Map[String, PlanTemplate] = all.map(t => t.key -> t).toMap

  def find(key: String): Option[PlanTemplate] = byKey.get(key)
