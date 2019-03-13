def containerFolder = "${PROJECT_NAME}/Cloud_Provision/IaaS/Pre-Cloning"
def createJob = freeStyleJob(containerFolder + '/Database_Precloning')

createJob.with {
    description('')
    parameters {
        stringParam('HOSTNAME', '', '')
		stringParam('DATABASE_NAME', '', '')
		stringParam('APPS_PASSWORD', '', '')
        stringParam('WEBLOGIC_PASSWORD', '', '')
		stringParam('APP_RUN_EDITION_DIR', '', '')
        stringParam('BACKUP_DIRECTORY', '', '')
		stringParam('NEXUS_TAG', '', '')
		stringParam('NEXUS_USERNAME', '', '')
		stringParam('NEXUS_PASSWORD', '', '')
		stringParam('ANSIBLE_CFG', '', '')
		stringParam('CUSTOM_WORKSPACE', '', '')
        stringParam('SSH_KEY', '', '')
        stringParam('TOLL_USERNAME', '', '')
        stringParam('TOLL_PASSWORD', '', '')
        stringParam('TOLL_PROXY', '', '')
    }

    logRotator {
        numToKeep(10)
        artifactNumToKeep(10)
    }

    concurrentBuild(true)
    label('postgres')
    customWorkspace('$CUSTOM_WORKSPACE')

    wrappers { 
        colorizeOutput('css')
    }

    steps {

        copyArtifacts('Set_Precloning_Parameters') {
            includePatterns('**/*')
            fingerprintArtifacts(true)
            buildSelector {
                upstreamBuild(true)
                latestSuccessful(false)
            }
        }

        shell('''#!/bin/bash

export ANSIBLE_CONFIG=${ANSIBLE_CFG}
export ANSIBLE_FORCE_COLOR=true

echo "#####################################################################################"
echo "# =====================> RUNNING DATABASE PRECLONING <============================= #"
echo "#####################################################################################"

ansible-playbook -i target-host --private-key=${CUSTOM_WORKSPACE}/${SSH_KEY} -u opc --become --become-user root \
-e "target=ebs db_name=${DATABASE_NAME} db_host=${HOSTNAME} apps_password=${APPS_PASSWORD} toll_user=${TOLL_USERNAME} \
toll_password=${TOLL_PASSWORD} toll_proxy=${TOLL_PROXY}" --tags "install,db_preclone" site.yml

		''')
	}

    publishers {

        archiveArtifacts('**/*')

        downstreamParameterized {
            trigger('Application_Precloning') {
                condition('SUCCESS')
                parameters {
                    currentBuild()
                }
            }
        }
    }
}


