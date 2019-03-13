def createJob = freeStyleJob("${PROJECT_NAME}/Cloud_Provision/IaaS/Pre-Cloning/Set_Precloning_Parameters")
def scmProject = "git@gitlab:${WORKSPACE_NAME}/Oracle_Tech.git"
def scmSSH = "git@gitlab:SSH/SSH_KEYS_PEM.git"
def scmCredentialsId = "adop-jenkins-master"

folder("${PROJECT_NAME}/Cloud_Provision") {
  configure { folder ->
    folder / icon(class: 'org.example.MyFolderIcon')
  }
}

folder("${PROJECT_NAME}/Cloud_Provision/IaaS") {
  configure { folder ->
    folder / icon(class: 'org.example.MyFolderIcon')
  }
}

folder("${PROJECT_NAME}/Cloud_Provision/IaaS/Pre-Cloning") {
  configure { folder ->
    folder / icon(class: 'org.example.MyFolderIcon')
  }
}

Closure passwordParam(String paramName, String paramDescription, String paramDefaultValue) {
    return { project ->
        project / 'properties' / 'hudson.model.ParametersDefinitionProperty' / 'parameterDefinitions' << 'hudson.model.PasswordParameterDefinition' {
            'name'(paramName)
      		'description'(paramDescription)
        	'defaultValue'(paramDefaultValue)
        }
    }
}

createJob.with {
    description('')
    parameters {
      stringParam('HOSTNAME', '', 'Hostname of the server to run precloning. (eg. ebsdev1)')
      stringParam('IP_ADDRESS', '', 'Public IP address of the server to run precloning.')
		  stringParam('DATABASE_NAME', '', 'The Database SID.')
		  stringParam('APP_RUN_EDITION_DIR', '/u01/install/APPS/fs1', 'The Run File System that will point to fs1 or fs2 directory.')
      stringParam('BACKUP_DIRECTORY', '', 'Storage Directory for the Backup Files. (Note: Validate if there is enough disk space where the directory is located.')
      stringParam('SSH_KEY', '', 'SSH key of the Target Instance. (eg. rsa-key-tollgftxxx.pem)')
		  stringParam('NEXUS_TAG', 'ver1', 'The tag to be appended to the backup filename. (eg. <filename><tag>.tar.gz)')
		  stringParam('NEXUS_USERNAME', '', 'Nexus Account\'s username. (User must have access to the nexus repository)')
      stringParam('TOLL_USERNAME', '', 'Your Toll Account Username.')
      stringParam('TOLL_PROXY', '', 'Proxy Server to use for Internet Connection.')
    }
    configure passwordParam("TOLL_PASSWORD", "Your Toll Account Password", "")
		configure passwordParam("APPS_PASSWORD", "APPS Login Password.", "")
		configure passwordParam("WEBLOGIC_PASSWORD", "Weblogic Password.", "")
		configure passwordParam("NEXUS_PASSWORD", "Nexus account's password", "")

    logRotator {
        numToKeep(10)
        artifactNumToKeep(10)
    }

    concurrentBuild(true)
    label('postgres')
    customWorkspace('/var/jenkins_home/workspace/precloning/${BUILD_TAG}')

  multiscm {
    git {
      remote {
        url(scmSSH)
        credentials(scmCredentialsId)
        branch("*/master")
      }
      extensions {
        relativeTargetDirectory('ssh')
      }
    }
    git {     
      remote {
        url(scmProject)
        credentials(scmCredentialsId)
        branch("*/master")
      }
      extensions {
        relativeTargetDirectory('ansible')
      }
    }
  }

    wrappers {
        preBuildCleanup() 
        colorizeOutput('css')
    }

    steps {
		shell('''#!/bin/bash
mv ${WORKSPACE}/ansible/Cloud_Provision/IaaS/ebsr12-preclone/* .
mv ${WORKSPACE}/ssh/${SSH_KEY} .

rm -rf ansible ssh

cat > target-host <<-EOF
[ebs]
${IP_ADDRESS}
EOF

echo "CUSTOM_WORKSPACE=${WORKSPACE}" > props
echo "ANSIBLE_CFG=${WORKSPACE}/ssh_ansible.cfg" >> props

chmod 400 ${WORKSPACE}/${SSH_KEY}

		''')

        environmentVariables {
            propertiesFile('props')
        }
    }

    publishers {

        archiveArtifacts('**/*')

        downstreamParameterized {
            trigger('Database_Precloning') {
                condition('SUCCESS')
                parameters {
                    currentBuild()
                    propertiesFile('props', true)
                }
            }
        }
    }

}
