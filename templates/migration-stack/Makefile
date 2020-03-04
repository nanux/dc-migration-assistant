STACK_NAME := $(shell whoami)-$(shell date +"%d-%mt%H-%M-%S")-migration-helper

# Params for create-stack
# BUCKET: the S3 bucket to pull the database and file system from
# VPC_ID: the VPC to deploy the elastic beanstalk environment in
# SUBNET_IDS: the IDs of the subnets to distribute the elastic beanstalk EC2 instance between (There is only 1 instance so should just be a single subnet)
# EFS_ID: the file system ID of the EFS to sync the files to. Should be the EFS of the freshly deployed product stack
# SG_ID: The ID of the security group attached to the EFS, required to allow the beanstalk instance to communicate with the EFS for the duration of the migration

PARAMS := ParameterKey=HelperSourceBucket,ParameterValue=$(BUCKET) ParameterKey=HelperVpcId,ParameterValue=$(VPC_ID) ParameterKey=NetworkPrivateSubnets,ParameterValue=$(SUBNET_IDS) ParameterKey=EFSFileSystemId,ParameterValue=$(EFS_ID) ParameterKey=EFSSecurityGroup,ParameterValue=$(SG_ID)

packagesource:
	rm package/MigrationHelper.zip; cd src/MigrationHelper/; zip -r ../../package/MigrationHelper.zip .; cd ../..
shipsource:
	aws s3 cp package/MigrationHelper.zip s3://$(BUCKET)
createstack:
	aws cloudformation create-stack --stack-name $(STACK_NAME) --template-body file://migration-helper.yml --parameters $(PARAMS) --capabilities CAPABILITY_IAM --disable-rollback
deletestack:
	aws cloudformation delete-stack --stack-name $(STACK_NAME)
updatestack:
	aws cloudformation update-stack --stack-name $(STACK_NAME) --template-body file://migration-helper.yml --parameters $(PARAMS) --capabilities CAPABILITY_IAM
