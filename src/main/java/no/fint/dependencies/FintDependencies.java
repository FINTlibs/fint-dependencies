package no.fint.dependencies;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class FintDependencies {
    @Value("${fint.dependencies.report-file:report.json}")
    private String reportFile;

    @Value("${fint.dependencies.json-path-current:$.current.dependencies}")
    private String jsonPathCurrent;

    @Value("${fint.dependencies.json-path.outdated:$.outdated.dependencies}")
    private String jsonPathOutdated;

    private List<Dependency> current = new ArrayList<>();
    private List<Dependency> outdated = new ArrayList<>();

    public FintDependencies() {
        Configuration.setDefaults(new Configuration.Defaults() {
            private final JsonProvider jsonProvider = new JacksonJsonProvider();
            private final MappingProvider mappingProvider = new JacksonMappingProvider();

            @Override
            public JsonProvider jsonProvider() {
                return jsonProvider;
            }

            @Override
            public Set<Option> options() {
                return EnumSet.noneOf(Option.class);
            }

            @Override
            public MappingProvider mappingProvider() {
                return mappingProvider;
            }
        });
    }

    @PostConstruct
    public void init() {
        try {
            URL resource = Resources.getResource(FintDependencies.class, String.format("/%s", reportFile));
            String json = FileUtils.readFileToString(new File(resource.toURI()), Charsets.UTF_8);
            current = load(json, jsonPathCurrent);
            outdated = load(json, jsonPathOutdated);
        } catch (IllegalArgumentException | IOException | URISyntaxException e) {
            log.info("Unable to read report.json file, {}", e.getMessage());
        }
    }

    private List<Dependency> load(String json, String path) {
        Dependency[] dependencies = JsonPath.parse(json).read(path, Dependency[].class);
        return Arrays.asList(dependencies);
    }

    public List<Dependency> getAll() {
        return current;
    }

    public List<Dependency> getFintDependencies() {
        return current.stream().filter(dependency -> dependency.getGroup().contains("no.fint")).collect(Collectors.toList());
    }

    public List<Dependency> getOutdated() {
        return outdated;
    }
}
