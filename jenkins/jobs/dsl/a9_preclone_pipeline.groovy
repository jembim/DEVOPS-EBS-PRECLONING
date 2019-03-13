def containerFolder = "${PROJECT_NAME}/Cloud_Provision/IaaS/Pre-Cloning"

buildPipelineView(containerFolder + '/Pre_Cloning_Pipeline') {
    title('Environment_Precloning')
    displayedBuilds(10)
    selectedJob('Set_Precloning_Parameters')
	showPipelineDefinitionHeader()
    showPipelineParameters()
	consoleOutputLinkStyle(OutputStyle.NewWindow)
    refreshFrequency(3)
}