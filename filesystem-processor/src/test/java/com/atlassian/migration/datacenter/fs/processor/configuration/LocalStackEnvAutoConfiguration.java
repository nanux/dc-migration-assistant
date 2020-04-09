package com.atlassian.migration.datacenter.fs.processor.configuration;

import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.CreateStackResult;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.model.ParameterType;
import com.amazonaws.services.simplesystemsmanagement.model.PutParameterRequest;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.cloud.aws.core.env.stack.config.StackResourceRegistryFactoryBean;
import org.springframework.cloud.aws.core.env.stack.config.StaticStackNameProvider;
import org.springframework.cloud.aws.core.region.RegionProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;

import java.io.File;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
public class LocalStackEnvAutoConfiguration {

    private final static String STACK_NAME = "migration-helper";


    @Bean
    @SneakyThrows
    public CreateStackResult setupCloudFormation(AmazonCloudFormation amazonCloudFormation) {
        ClassLoader classLoader = getClass().getClassLoader();
        URL resource = classLoader.getResource("localstack.yml");
        File cfnTemplate = new File(resource.getFile());
        String templateBody = FileUtils.readFileToString(cfnTemplate, Charset.defaultCharset());
        CreateStackRequest createStackRequest = new CreateStackRequest().withStackName(STACK_NAME).withTemplateBody(templateBody);
        log.info("CloudFormation Stack Ready!");
        return amazonCloudFormation.createStack(createStackRequest);
    }

    @Bean
    public Map<String, String> setupSSMParameters(AWSSimpleSystemsManagement amazonSSM, RegionProvider regionProvider) {
        Map<String, String> params = new HashMap<>();
        params.put("/config/migration-helper/cloud.aws.stack.name", STACK_NAME);
        params.put("/config/migration-helper/app.jira.file.path", "atl-migration-queue-migration-helper");
        params.put("/config/migration-helper/app.vpc.id", "vpc-02a66f2c3b0b1f599");
        params.put("/config/migration-helper/app.region.id", regionProvider.getRegion().getName());

        for (String key : params.keySet()) {
            String[] keyElements = key.split("/");
            PutParameterRequest putParameterRequest = new PutParameterRequest()
                    .withType(ParameterType.String)
                    .withName(key)
                    .withValue(params.get(key))
                    .withDescription(keyElements[keyElements.length - 1]);
            amazonSSM.putParameter(putParameterRequest);
        }
        return params;
    }

    @Bean
    @Primary
    @DependsOn({"setupSSMParameters", "setupCloudFormation"})
    public StackResourceRegistryFactoryBean stackResourceRegistryFactoryBean(AmazonCloudFormation amazonCloudFormation) {
        return new StackResourceRegistryFactoryBean(amazonCloudFormation, new StaticStackNameProvider(STACK_NAME));
    }


}
