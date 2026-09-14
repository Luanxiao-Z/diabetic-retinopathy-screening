package cn.edu.fzu.drs.module.common.result;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.io.Serializable;
import java.util.List;

/**
 * 统一分页响应结构：{total, current, pageSize, list}（对齐《后端应用开发规范》3.4 分页示例）。
 */
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private long total;
    private long current;
    private long pageSize;
    private List<T> list;

    public PageResult() {
    }

    public static <T> PageResult<T> of(IPage<T> page) {
        PageResult<T> result = new PageResult<>();
        result.total = page.getTotal();
        result.current = page.getCurrent();
        result.pageSize = page.getSize();
        result.list = page.getRecords();
        return result;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getCurrent() {
        return current;
    }

    public void setCurrent(long current) {
        this.current = current;
    }

    public long getPageSize() {
        return pageSize;
    }

    public void setPageSize(long pageSize) {
        this.pageSize = pageSize;
    }

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }
}
