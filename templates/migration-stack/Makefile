STACK_NAME := $(shell whoami)-$(shell date +"%d-%mt%H-%M-%S")-migration-helper

packagesource:
	rm package/MigrationHelper.zip; cd src/MigrationHelper/; zip -r ../../package/MigrationHelper.zip .; cd ../..
shipsource:
	aws s3 cp package/MigrationHelper.zip s3://$(BUCKET)
createstack:
	aws cloudformation create-stack --stack-name $(STACK_NAME) --template-body file://migration-helper.yml --parameters ParameterKey=HelperSourceBucket,ParameterValue=$(BUCKET) ParameterKey=HelperVpcId,ParameterValue=$(VpcID) --capabilities CAPABILITY_IAM --disable-rollback
deletestack:
	aws cloudformation delete-stack --stack-name $(STACK_NAME)
updatestack:
	aws cloudformation update-stack --stack-name $(STACK_NAME) --template-body file://migration-helper.yml --parameters ParameterKey=HelperSourceBucket,ParameterValue=$(BUCKET) ParameterKey=HelperVpcId,ParameterValue=$(VpcID) --capabilities CAPABILITY_IAM
