package its.time.tracker.domain

import its.time.tracker.config.Constants

data class CostAssessmentSetup (
    val developmentProjects: List<CostAssessmentProject>,
    val maintenanceProjects: List<CostAssessmentProject>,
    val internalProjects: List<CostAssessmentProject>,
    val absenceProjects: List<CostAssessmentProject>,
) {

    companion object {

        const val DEFAULT_DEV_PROJECT = "Project Placeholder"
        const val DEFAULT_MAINT_PROJECT = "Wartung"
        const val DEFAULT_INT_PROJECT = "Line Activity"
        const val DEFAULT_ABSC_PROJECT = "Other absence"

        fun getEmptyInstance(): CostAssessmentSetup {
            return CostAssessmentSetup(
                developmentProjects = emptyList(),
                maintenanceProjects = emptyList(),
                internalProjects = emptyList(),
                absenceProjects = emptyList()
            )
        }
    }

    fun getOfficialProjectName(projectInput: String): String {
        val unifiedList = developmentProjects + maintenanceProjects + internalProjects + absenceProjects
        return unifiedList.firstOrNull { it.abbreviation == projectInput }?.title ?: projectInput
    }

    fun getProjectCategory(project: String): String {
        val map = mapOf(
            Constants.COST_ASSMNT_DEV_KEY to developmentProjects,
            Constants.COST_ASSMNT_MAINT_KEY to maintenanceProjects,
            Constants.COST_ASSMNT_INT_KEY to internalProjects,
            Constants.COST_ASSMNT_ABSC_KEY to absenceProjects,
        )

        map.forEach { (key, projectList) ->
            if (projectList.map { it.title }.contains(project)) {
                return key
            }
        }

        return "no-project"
    }

    fun getDefaultProjectFor(superiorProject: String): String {
        return when (superiorProject) {
            Constants.COST_ASSMNT_DEV_KEY -> DEFAULT_DEV_PROJECT
            Constants.COST_ASSMNT_MAINT_KEY -> DEFAULT_MAINT_PROJECT
            Constants.COST_ASSMNT_INT_KEY -> DEFAULT_INT_PROJECT
            Constants.COST_ASSMNT_ABSC_KEY -> DEFAULT_ABSC_PROJECT
            else -> "-error-"
        }
    }
}