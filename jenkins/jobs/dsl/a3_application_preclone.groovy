def containerFolder = "${PROJECT_NAME}/Cloud_Provision/IaaS/Pre-Cloning"
def createJob = freeStyleJob(containerFolder + '/Application_Precloning')

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

        copyArtifacts('Database_Precloning') {
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
echo "# ====================> RUNNING APPLICATION PRECLONING <=========================== #"
echo "#####################################################################################"

ansible-playbook -i target-host --private-key=${CUSTOM_WORKSPACE}/${SSH_KEY} -u opc --become --become-user root -e "target=ebs db_name=${DATABASE_NAME} app_run_dir=${APP_RUN_EDITION_DIR} \
db_host=${HOSTNAME} apps_password=${APPS_PASSWORD} wls_password=${WEBLOGIC_PASSWORD}" --tags "app_preclone" site.yml

		''')
	}

    publishers {

        archiveArtifacts('**/*')

        downstreamParameterized {
            trigger('Shutdown_Services') {
                condition('SUCCESS')
                parameters {
                    currentBuild()
                }
            }
        }
    }
}