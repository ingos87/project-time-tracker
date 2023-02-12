package its.time.tracker.domain

import its.time.tracker.config.Constants

data class CostAssessmentSetup (
    val developmentProjects: List<CostAssessmentProject>,
    val maintenanceProjects: List<CostAssessmentProject>,
    val internalProjects: List<CostAssessmentProject>,
    val absenceProjects: List<CostAssessmentProject>,
) {

    companion object {
        fun getEmptyInstance(): CostAssessmentSetup {
            return CostAssessmentSetup(
                developmentProjects = emptyList(),
                maintenanceProjects = emptyList(),
                internalProjects = emptyList(),
                absenceProjects = emptyList()
            )
        }
    }
    fun resolveTopicToProject(topic: String): String {
        if (topic.startsWith("EDF-")) {
            return "Wartung"
        }
        if (topic.startsWith("DVR-")) {
            return "Line Activity"
        }

        (developmentProjects + maintenanceProjects + internalProjects + absenceProjects)
            .forEach { project ->
            if (project.possibleTopics.any { it.equals(topic, ignoreCase = true) }) {
                return project.title
            }
        }

        println("Found no fitting booking position for work topic '$topic' -> using cost assessment 'Project Placeholder'")
        return "Project Placeholder"
    }

    fun getSuperiorProject(project: String): String {
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

        return Constants.COST_ASSMNT_DEV_KEY
    }
}