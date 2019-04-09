package com.hisense.hitsp.excel;

import com.hisense.hitsp.common.CommonUtil;
import com.hisense.hitsp.common.HitspBizException;
import com.hisense.hitsp.model.BizReturnCode;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by user on 2017/10/24.
 */
@Component
public class ProductExcelImpl implements Excel {
    Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public Map<String, Object> getExcelData(MultipartFile file) throws HitspBizException, IOException, InvalidFormatException {
        Map<String, Object> result = new HashMap<>();
        List resultlist = new LinkedList();
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
        DecimalFormat df1 = new DecimalFormat("0.0000");
        Workbook workbook = null;
        Sheet sheet = null;
        try {
            workbook = CommonUtil.getWorkbook(file);
            sheet = workbook.getSheetAt(0);
            workbook.close();
            int rowCount = sheet.getPhysicalNumberOfRows(); //获取总行数
            List<String> cloumNameList = new LinkedList();
            //处理第一行
            Row firstrow = sheet.getRow(0);
            firstrow.cellIterator().forEachRemaining((cell) -> {
                int idx = cell.getColumnIndex();
                String cellValue = cell.getStringCellValue();
                cloumNameList.add(idx, cloumDispose(cellValue));
            });
            //遍历每一行
            for (int r = 1; r < rowCount; r++) {
                Map<String, Object> onemap = new HashMap<>();
                for (int i = 0; i < cloumNameList.size(); i++) {
                    onemap.put(cloumNameList.get(i), "");
                }
                Row row = sheet.getRow(r);
                //遍历每一列
                for (int i = 0; i < onemap.size(); i++) {
                    Cell cell = row.getCell(i);
                    String cellValue = "";
                    if (null != cell) {
                        int cellType = cell.getCellType();
                        switch (cellType) {
                            case Cell.CELL_TYPE_NUMERIC: //数字、日期
                                if (DateUtil.isCellDateFormatted(cell)) {
                                    cellValue = fmt.format(cell.getDateCellValue()); //日期型
                                } else {
//                                    cellValue = df1.format(cell.getNumericCellValue()); //数字
                                    cell.setCellType(Cell.CELL_TYPE_STRING);
                                    cellValue = String.valueOf(cell.getRichStringCellValue().getString());
                                }
                                break;
                            case Cell.CELL_TYPE_STRING: //文本
                                cellValue = cell.getStringCellValue();
                                break;
                            case Cell.CELL_TYPE_BLANK: //空白
                                cellValue = cell.getStringCellValue();
                                break;
                            default:
                                cellValue = "错误";
                        }

                        int h = r + 1;
                        int l = i + 1;
                        if (StringUtils.equals(cellValue, "错误")) {
                            throw new HitspBizException(BizReturnCode.ExcelError, "导入失败，解析类型错误，请将单元格格式设置为文本");
                        } else if (i == 0) {
                            if (StringUtils.equals(cellValue, "")) {
                                throw new HitspBizException(BizReturnCode.ExcelError, "导入失败，第" + h + "行，第" + i + "列不可为空");
                            }
                            if (cellValue.length() > 20) {
                                throw new HitspBizException(BizReturnCode.ExcelError, "导入失败，第" + h + "行，第" + l + "列超长，限制长度为20");
                            }
                        } else if (i == 1) {
                            if (StringUtils.equals(cellValue, "")) {
                                throw new HitspBizException(BizReturnCode.ExcelError, "导入失败，第" + h + "行，第" + l + "列不可为空");
                            }
                            if (cellValue.length() > 50) {
                                throw new HitspBizException(BizReturnCode.ExcelError, "导入失败，第" + h + "行，第" + l + "列超长，限制长度为50");
                            }
                        } else if (i == 2 || i == 3) {
                            if (StringUtils.equals(cellValue, "")) {
                                throw new HitspBizException(BizReturnCode.ExcelError, "导入失败，第" + h + "行，第" + l + "列不可为空");
                            }
                            if (cellValue.length() > 20) {
                                throw new HitspBizException(BizReturnCode.ExcelError, "导入失败，第" + h + "行，第" + l + "列超长，限制长度为20");
                            }
                        } else if (i == 4 || i == 8) {
                            if (cellValue.length() > 200) {
                                throw new HitspBizException(BizReturnCode.ExcelError, "导入失败，第" + h + "行，第" + l + "列超长，限制长度为200");
                            }
                        } else if (i == 5) {
                            if (cellValue.length() > 20) {
                                throw new HitspBizException(BizReturnCode.ExcelError, "导入失败，第" + h + "行，第" + l + "列超长，限制长度为20");
                            }
                        } else if (i == 6) {
                            if (cellValue.length() > 50) {
                                throw new HitspBizException(BizReturnCode.ExcelError, "导入失败，第" + h + "行，第" + l + "列超长，限制长度为50");
                            }
                        } else if (i == 7) {
                            if (cellValue.contains(".")) {
                                cellValue = cellValue.replaceAll("0+?$", "");
                                if (cellValue.length() - (cellValue.indexOf(".") + 1) > 2) {
                                    throw new HitspBizException(BizReturnCode.ExcelError, "导入失败，第" + h + "行，第" + l + "列小数位数不可大于2");
                                }
                            }
                        } else if (i == 9 || i == 10) {
                            if (cellValue.length() > 45) {
                                throw new HitspBizException(BizReturnCode.ExcelError, "导入失败，第" + h + "行，第" + l + "列超长，限制长度为45");
                            }
                            if (cellValue.contains(".") && cellValue.length() - (cellValue.indexOf(".") + 1) > 3) {
                                throw new HitspBizException(BizReturnCode.ExcelError, "导入失败，第" + h + "行，第" + l + "列小数位数不可大于3");
                            }
                        }
                    }
                    onemap.put(cloumNameList.get(i), cellValue);
                }
                resultlist.add(onemap);
            }
        } catch (IOException e) {
            logger.error("处理excel数据异常", e);
            throw e;
        } catch (InvalidFormatException e) {
            logger.error("处理excel数据异常", e);
            throw e;
        }
        result.put("resultlist", resultlist);
        return result;
    }


    private static String cloumDispose(String cellValue) {
        String cloumname = "";
        switch (cellValue) {
            case "商品ID":
                cloumname = "id";
                break;
            case "商品名称":
                cloumname = "name";
                break;
            case "商店ID":
                cloumname = "shopId";
                break;
            case "品类ID":
                cloumname = "categoryId";
                break;
            case "商品描述":
                cloumname = "description";
                break;
            case "规格ID":
                cloumname = "specId";
                break;
            case "规格名称":
                cloumname = "specName";
                break;
            case "商品价格":
                cloumname = "price";
                break;
            case "备注":
                cloumname = "remark";
                break;
            case "包装换算率":
                cloumname = "unitWeight";
                break;
            case "单位重量(kg)":
                cloumname = "weight";
                break;
        }
        return cloumname;
    }
}
