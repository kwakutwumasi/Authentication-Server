import React, { Component } from 'react';
import {Link} from "react-router-dom";

import Grid from "@material-ui/core/Grid";

import Checkbox from "@material-ui/core/Checkbox"
import TextField from "@material-ui/core/TextField";
import Select from "@material-ui/core/Select";

import Paper from  '@material-ui/core/Paper';
import Table from "@material-ui/core/Table";
import TableBody from "@material-ui/core/TableBody";
import TableCell from "@material-ui/core/TableCell";
import TableRow from "@material-ui/core/TableRow";
import MenuItem from "@material-ui/core/MenuItem";

import IconButton from "@material-ui/core/IconButton";
import Button from "@material-ui/core/Button";
import SearchIcon from '@material-ui/icons/Search';
import ChevronLeftIcon from '@material-ui/icons/ChevronLeft';
import ChevronRightIcon from '@material-ui/icons/ChevronRight';
import CircularProgress from "@material-ui/core/CircularProgress";
import AddAliasSVG from "../images/addalias.svg"
import RemoveAliasSVG from "../images/removealias.svg"
import LockDeviceSVG from "../images/lockdevice.svg"
import UnLockDeviceSVG from "../images/unlockdevice.svg"
import DeactivateDeviceSVG from "../images/deactivatedevice.svg"
import GroupAddIcon from '@material-ui/icons/GroupAdd';
import PermIdentityIcon from '@material-ui/icons/PermIdentity';

const statusValues = [		{label:"Initiated", value:"INITIATED"},
		{label:"Active", value:"ACTIVE"},
		{label:"Locked", value:"LOCKED"},
		{label:"Inactive", value:"INACTIVE"}];

const iconHeight = 30;

class DeviceCheckBox extends Component {
  constructor(props){
    super(props);
    this.state = {checked:false};
  }

  itemCheck=(event)=>{
    var currentChecked = this.state.checked;
    this.setState({checked:!currentChecked});
    this.props.itemCheck(this.props.device.id,!currentChecked);
  }

  render(){
    return (
      <Checkbox checked={this.state.checked || this.props.allChecked} onChange={this.itemCheck} />
    );
  }
}

const HomePage = (props) => {
  const {classes, previousActive, nextActive} = props;
  const emptyRows = props.maxrows-props.devices.length;

  return (
    <Grid container spacing={2}>
    <Grid item xs={false} sm={false} md={false} lg={2} />
    <Grid item xs={12} sm={12} md={12} lg={8}>
    <Paper className={classes.homePaperFirst}>
      <Table className={classes.table}>
        <TableBody>
            <TableRow>
              <TableCell variant="head" align="left"><h3>Total Device Count</h3></TableCell><TableCell variant="body" align="left">100</TableCell>
            </TableRow>
        </TableBody>
      </Table>
    </Paper>
    <Paper className={classes.homePaper}><h3 align="center">Devices</h3>
      <Table className={classes.table}>
        <TableBody>
            <TableRow>
              <TableCell variant="head" align="left"><Checkbox checked={props.allChecked} onChange={()=>props.allCheckboxChanged()}  /></TableCell>
              <TableCell variant="head" align="left">Device ID</TableCell><TableCell className={classes.tableCellHiddenSm} variant="head" align="left">Aliases</TableCell><TableCell variant="head" align="left">Status</TableCell>
            </TableRow>
            {props.devices.map((device, index) => {
              return <TableRow key={device.itemCount} className={classes.tableRow}>
                      <TableCell variant="head" align="left"><DeviceCheckBox device={device} itemCheck={props.itemCheck} allChecked={props.allChecked}  /></TableCell>
                      <TableCell variant="body" align="left">{device.id}</TableCell><TableCell className={classes.tableCellHiddenSm} variant="body" align="left">{device.aliases.join("; ")}</TableCell><TableCell variant="body" align="left">{device.status}</TableCell>
                    </TableRow>;
            })}
            {emptyRows > 0 && (
              <TableRow style={{ height: 57 * emptyRows }}>
                <TableCell colSpan={6} />
              </TableRow>
            )}
        </TableBody>
      </Table>
      <Grid container spacing={2}>
        <Grid item sm={12} md={9}>
          <IconButton title="Assign Aliases" variant="contained" component={Link} to="/assign">
            <img src={AddAliasSVG} alt="Assign Aliases" height={iconHeight} />
          </IconButton>
          <IconButton title="Assign Aliases" variant="contained" component={Link} to="/assign">
            <img src={AddAliasSVG} alt="Assign Aliases" height={iconHeight} />
          </IconButton>
          <IconButton title="Un-assign Aliases" variant="contained" component={Link} to="/unassign">
            <img src={RemoveAliasSVG} alt="Un-assign Aliases" height={iconHeight} />
          </IconButton>
          <IconButton title="Lock Devices" variant="contained" component={Link} to="/lock">
            <img src={LockDeviceSVG} alt="Lock Devices" height={iconHeight} />
          </IconButton>
          <IconButton title="Un-lock Devices" variant="contained" component={Link} to="/unlock">
            <img src={UnLockDeviceSVG} alt="Un-lock Devices" height={iconHeight} />
          </IconButton>
          <IconButton title="De-activate Devices" variant="contained" component={Link} to="/deactivate">
            <img src={DeactivateDeviceSVG} alt="De-activate Devices" height={iconHeight} />
          </IconButton>
          <IconButton title="Add Administrator Devices" variant="contained" component={Link} to="/addadmin">
            <GroupAddIcon />
          </IconButton>
          <IconButton title="Remove Administrator Devices" variant="contained" component={Link} to="/removeadmin">
            <PermIdentityIcon />
          </IconButton>
        </Grid>
        <Grid item sm={12} md={3}>
        <div className={classes.paginationRight}>
          <Select
            value={props.maxrows}
            onChange={event => props.valueChanged(event,"maxrows")}
            inputProps={{
                name: 'rowCount',
                id: 'rowCount-input',
          }}>
            <MenuItem value={5}>5</MenuItem>
            <MenuItem value={10}>10</MenuItem>
            <MenuItem value={20}>20</MenuItem>
            <MenuItem value={30}>30</MenuItem>
          </Select>
          <IconButton variant="contained" color="default" disabled={!previousActive}
            onClick={event=> props.previous(event)} aria-label="Edit">
            <ChevronLeftIcon />
          </IconButton>
          <IconButton variant="contained" color="default" disabled={!nextActive}
            onClick={event=> props.next(event)} aria-label="Edit">
            <ChevronRightIcon />
          </IconButton>
        </div>
        </Grid>
        <Grid item xs={12}>
          <h3>Search Terms</h3>
          <div>
          <TextField label="Device Status"
            fullWidth={true}
            select
            value={props.status}
            className={classes.searchField}
            margin="normal"
            onChange={event => props.valueChanged(event,"status")}>
            {statusValues.map(option => (
            <MenuItem key={option.value} value={option.value}>
                {option.label}
            </MenuItem>
            ))}
          </TextField>
          </div>
          <div>
          <Button variant="contained" color="default" className={classes.button}
            onClick={event=> props.search(event)} aria-label="Search">
             {props.loading? <CircularProgress size={24} className={classes.buttonProgress} />:<SearchIcon />} Search
          </Button>
          </div>
        </Grid>
      </Grid>
    </Paper>
    <Paper className={classes.homePaper}><h3 align="center">Administrators</h3>
      <Table className={classes.table}>
        <TableBody>
            <TableRow>
              <TableCell variant="head" align="left">Name</TableCell><TableCell variant="head" align="left">Device ID</TableCell><TableCell  className={classes.tableCellHiddenSm} variant="head" align="left">Status</TableCell>
            </TableRow>
            {props.administrators.map((administrator, index) => {
              return <TableRow key={index} className={classes.tableRow}>
                      <TableCell variant="body" align="left">{administrator.deviceId}</TableCell>
                      <TableCell variant="body" align="left">{administrator.commonName}</TableCell>
                      <TableCell className={classes.tableCellHiddenSm} variant="body" align="left">{administrator.deviceStatus}</TableCell>
                    </TableRow>;
            })}
        </TableBody>
      </Table>
    </Paper>
  </Grid>
  </Grid>
);
}

export default HomePage;
