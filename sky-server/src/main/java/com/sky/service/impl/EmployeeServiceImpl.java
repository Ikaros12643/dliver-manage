package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sky.constant.MessageConstant;
import com.sky.constant.PasswordConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.exception.AccountLockedException;
import com.sky.exception.AccountNotFoundException;
import com.sky.exception.PasswordErrorException;
import com.sky.mapper.EmployeeMapper;
import com.sky.result.PageResult;
import com.sky.service.EmployeeService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeMapper employeeMapper;

    /**
     * 员工登录
     *
     * @param employeeLoginDTO
     * @return
     */
    public Employee login(EmployeeLoginDTO employeeLoginDTO) {
        System.out.println("当前线程的id: "+ Thread.currentThread().getId());

        String username = employeeLoginDTO.getUsername();
        String password = employeeLoginDTO.getPassword();

        //1、根据用户名查询数据库中的数据
        LambdaQueryWrapper<Employee> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Employee::getUsername, employeeLoginDTO.getUsername());
//        Employee employee = employeeMapper.getByUsername(username);
        Employee employee = employeeMapper.selectOne(lqw);


        //2、处理各种异常情况（用户名不存在、密码不对、账号被锁定）
        if (employee == null) {
            //账号不存在
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        //密码比对
        //后期需要进行md5加密，然后再进行比对
        //对前端传来的明文密码进行加密处理
        password = DigestUtils.md5DigestAsHex(password.getBytes());
        if (!password.equals(employee.getPassword())) {
            //密码错误
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }

        if (employee.getStatus() == StatusConstant.DISABLE) {
            //账号被锁定
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }

        //3、返回实体对象
        return employee;
    }

    /**
     * 新增员工
     * @param employeeDTO
     */
    @Override
    public void save(EmployeeDTO employeeDTO) {
        Employee employee = new Employee();
        //对象属性拷贝
        //从前面的对象拷贝到后面的对象
        BeanUtils.copyProperties(employeeDTO, employee);

        //设置账号的状态，默认为1 1表示正常 0表示禁用
        employee.setStatus(StatusConstant.ENABLE);

        //设置密码，默认123456
        employee.setPassword(DigestUtils.md5DigestAsHex(PasswordConstant.DEFAULT_PASSWORD.getBytes()));

        //设置当前记录的创建时间和修改时间
        employee.setCreateTime(LocalDateTime.now());
        employee.setUpdateTime(LocalDateTime.now());

        //设置当前纪录创建人和修改人ID
        //后期需要改为当前登陆用户的id
        Long currentId = BaseContext.getCurrentId();
        employee.setCreateUser(currentId);
        employee.setUpdateUser(currentId);
        BaseContext.removeCurrentId();
        employeeMapper.save(employee);
    }

    /**
     * 员工分页查询及姓名模糊查询
     * @param employeePageQueryDTO
     * @return
     */
    @Override
    public PageResult list(EmployeePageQueryDTO employeePageQueryDTO) {
        LambdaQueryWrapper<Employee> lqw = new LambdaQueryWrapper<>();
        lqw.like(null!=employeePageQueryDTO.getName(), Employee::getName, employeePageQueryDTO.getName());

        //创建mp分页对象
        IPage<Employee> page = new Page<>(employeePageQueryDTO.getPage(), employeePageQueryDTO.getPageSize());
        //向mp分页对象中存入查询结果
        employeeMapper.selectPage(page, lqw);

       /* PageHelper.startPage(employeePageQueryDTO.getPage(), employeePageQueryDTO.getPageSize());
        List<Employee> list = employeeMapper.selectList(lqw);
        System.out.println(list);
        Page<Employee> p = (Page<Employee>) list;
        */
        PageResult res = new PageResult(page.getTotal(), page.getRecords());
        return  res;
    }

    @Override
    public void startOrStop(Integer status, Long id) {
//        Employee e = new Employee();
//        e.setStatus(status);
//        e.setId(id);

        Employee e = Employee.builder()
                .status(status)
                .id(id).build();

        employeeMapper.updateById(e);
    }
}
