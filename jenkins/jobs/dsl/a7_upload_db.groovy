def containerFolder = "${PROJECT_NAME}/Cloud_Provision/IaaS/Pre-Cloning"
def createJob = freeStyleJob(containerFolder + '/Nexus_Upload_Database')

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

        copyArtifacts('Archive_Database') {
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
echo "# =================> UPLOADING DATABASE BINARIES TO NEXUS <======================== #"
echo "#####################################################################################"

ansible-playbook -i target-host --private-key=${CUSTOM_WORKSPACE}/${SSH_KEY} -u opc --become --become-user root -e "target=ebs app_run_dir=${APP_RUN_EDITION_DIR} \
tag=${NEXUS_TAG} nexus_user=${NEXUS_USERNAME} nexus_password=${NEXUS_PASSWORD} backup_dir=${BACKUP_DIRECTORY}" --tags "db_upload" site.yml

		''')
	}

}