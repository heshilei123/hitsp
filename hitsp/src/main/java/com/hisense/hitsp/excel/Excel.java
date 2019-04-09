package com.hisense.hitsp.excel;

import com.hisense.hitsp.common.HitspBizException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Created by user on 2017/10/24.
 */
public interface Excel {
    Map<String,Object> getExcelData(MultipartFile data) throws HitspBizException, IOException, InvalidFormatException;
}
