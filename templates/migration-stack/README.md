# Migration Template

Deploys resources required to facilitate the migration. Develop with Make

## Make createstack

Deploys the stack for testing. You must be authenticated with AWS. You should define the following variables:

* VPC\_ID - the VPC to deploy the stack in
* SUBNET\_ID - the Subnet to deploy the ASG in (should have mountpoint for the app EFS)
* EFS\_ID - the FS ID of the app EFS
* SG\_ID - the security group attached to the EFS. It will be opened up to allow the EC2 instances to mount it

## Make rendertemplate

Renders the file copy status python script into the migration template. Required to launch the template. Commit both the source and the rendered template to the repository for now
