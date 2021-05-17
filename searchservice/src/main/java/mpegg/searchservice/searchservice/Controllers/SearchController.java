package mpegg.searchservice.searchservice.Controllers;

import mpegg.searchservice.searchservice.Models.Dataset;
import mpegg.searchservice.searchservice.Models.DatasetGroup;
import mpegg.searchservice.searchservice.Models.MPEGFile;
import mpegg.searchservice.searchservice.Repositories.DatasetGroupRepository;
import mpegg.searchservice.searchservice.Repositories.DatasetRepository;
import mpegg.searchservice.searchservice.Repositories.MPEGFileRepository;
import mpegg.searchservice.searchservice.Repositories.SampleRepository;
import mpegg.searchservice.searchservice.Utils.AuthorizationUtil;
import mpegg.searchservice.searchservice.Utils.JWTUtil;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class SearchController {

    private final String urlGCS = "http://localhost:8082";

    private JWTUtil jwtUtil = new JWTUtil();
    @Autowired
    MPEGFileRepository mpegFileRepository;

    @Autowired
    DatasetGroupRepository datasetGroupRepository;

    @Autowired
    DatasetRepository datasetRepository;

    @Autowired
    SampleRepository sampleRepository;

    private final AuthorizationUtil authorizationUtil = new AuthorizationUtil();

    @GetMapping("/get")
    private String get() {
        return "a";
    }

    @GetMapping("/ownFiles")
    private ResponseEntity<JSONArray> ownFiles(@AuthenticationPrincipal Jwt jwt) {
        String ownerId = jwtUtil.getUID(jwt);
        List<MPEGFile> list = mpegFileRepository.findByOwner(ownerId);
        JSONArray response = new JSONArray();
        for (MPEGFile file : list) {
            JSONObject aux = new JSONObject();
            aux.appendField("name", file.getName());
            aux.appendField("id", file.getId());
            response.add(aux);
        }
        return new ResponseEntity<JSONArray>(response, HttpStatus.OK);
    }

    @GetMapping("/ownDatasetGroup")
    private ResponseEntity<JSONArray> ownDatasetGroup(@AuthenticationPrincipal Jwt jwt) {
        String ownerId = jwtUtil.getUID(jwt);
        List<DatasetGroup> list = datasetGroupRepository.findByOwner(ownerId);
        JSONArray response = new JSONArray();
        for (DatasetGroup dg : list) {
            JSONObject aux = new JSONObject();
            aux.appendField("id", dg.getId());
            response.add(aux);
        }
        return new ResponseEntity<JSONArray>(response, HttpStatus.OK);
    }

    @GetMapping("/ownDataset")
    private ResponseEntity<JSONArray> ownDataset(@AuthenticationPrincipal Jwt jwt) {
        String ownerId = jwtUtil.getUID(jwt);
        List<Dataset> list = datasetRepository.findByOwner(ownerId);
        JSONArray response = new JSONArray();
        for (Dataset dt : list) {
            JSONObject aux = new JSONObject();
            aux.appendField("id", dt.getId());
            response.add(aux);
        }
        return new ResponseEntity<JSONArray>(response, HttpStatus.OK);
    }

    @PostMapping("/findDatasetGroupMetadata")
    private ResponseEntity<JSONArray> findDatasetGroupMetadata(@AuthenticationPrincipal Jwt jwt,
                                                               @RequestParam(value = "center", required = false) String center,
                                                               @RequestParam(value = "description", required = false) String description,
                                                               @RequestParam(value = "title", required = false) String title,
                                                               @RequestParam(value = "type", required = false) String type)
    {
        JSONArray response = new JSONArray();
        center = (center == null) ? null : "%"+center+"%";
        description = (description == null) ? null : "%"+description+"%";
        title = (title == null) ? null :"%"+title+"%";
        type = (type == null) ? null :"%"+type+"%";
        List<DatasetGroup> list = datasetGroupRepository.findByMetadata(center,description,title,type);
        for (DatasetGroup dg : list) {
            if (authorizationUtil.authorized(urlGCS, "dg", String.valueOf(dg.getId()), jwt, "GetIdDatasetGroup", datasetGroupRepository, datasetRepository, mpegFileRepository)) {
                response.add(dg.getId());
            }
        }
        return new ResponseEntity<JSONArray>(response,HttpStatus.OK);
    }

    @PostMapping("/findDatasetMetadata")
    private ResponseEntity<JSONArray> findDatasetMetadata(@AuthenticationPrincipal Jwt jwt,
                                                          @RequestParam(value = "title", required = false) String title,
                                                          @RequestParam(value = "taxon_id", required = false) String taxon_id)
    {
        title = (title == null) ? null :"%"+title+"%";
        taxon_id = (taxon_id == null) ? null : "%"+taxon_id+"%";
        JSONArray response = new JSONArray();
        List<Long> datasetList = sampleRepository.findByMetadata(title,taxon_id);
        for (Long id : datasetList) {
            if (authorizationUtil.authorized(urlGCS, "dt", String.valueOf(id), jwt, "GetIdDataset", datasetGroupRepository, datasetRepository, mpegFileRepository)) {
                response.add(id);
            }
        }
        return new ResponseEntity<JSONArray>(response,HttpStatus.OK);
    }
}