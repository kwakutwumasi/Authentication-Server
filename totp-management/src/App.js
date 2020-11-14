import React, { Component } from 'react';
import { Route, Link, withRouter } from "react-router-dom";
import axios from "axios"
import { AppBar, Toolbar, Typography, Button, CircularProgress } from "@material-ui/core";
import { withStyles } from "@material-ui/core/styles";
import PropTypes from "prop-types";
import green from "@material-ui/core/colors/green";

import {TextField, Drawer, List, ListItem, ListItemIcon, ListItemText, Divider, Grid } from "@material-ui/core";

import HomeIcon from "@material-ui/icons/Home";
import ArrowForwardIosIcon from "@material-ui/icons/ArrowForwardIos";
import AddAliasSVG from "./images/addalias.svg"
import CloseIcon from '@material-ui/icons/Close';
import ExitToAppIcon from '@material-ui/icons/ExitToApp';
import RemoveAliasSVG from "./images/removealias.svg"
import LockDeviceSVG from "./images/lockdevice.svg"
import UnLockDeviceSVG from "./images/unlockdevice.svg"
import DeactivateDeviceSVG from "./images/deactivatedevice.svg"
import GroupAddIcon from '@material-ui/icons/GroupAdd';
import PermIdentityIcon from '@material-ui/icons/PermIdentity';
import MenuIcon from "@material-ui/icons/Menu";
import IconButton from "@material-ui/core/IconButton";
import LogoSVG from './images/logo.svg';
import AccountCircleIcon from '@material-ui/icons/AccountCircle';

import Snackbar from '@material-ui/core/Snackbar';

import Card from "@material-ui/core/Card";
import CardContent from "@material-ui/core/CardContent";
import CardActions from "@material-ui/core/CardActions";

import HomePage from "./components/HomePage.js"
import DeviceForm from "./components/DeviceFormComponent.js";

const apibase = window.totpapibaseurl;

const styles = theme => ({
  centered: {
    textAlign: "center"
  },
  toolbar: theme.mixins.toolbar,
  close: {
    padding: theme.spacing(1 / 2),
  },
  menuButton: {
    marginLeft: -12,
    marginRight: 20,
  },
  textField: {
    marginLeft: theme.spacing(1),
    marginRight: theme.spacing(1),
    minWidth: 300,
    display: "inline-block"
  },
  searchField: {
    marginLeft: theme.spacing(1),
    marginRight: theme.spacing(1),
    maxWidth: 300,
    display: "inline-block"
  },
  card: {
    marginTop: "5.0em",
    marginLeft: "auto",
    marginRight: "auto",
    opacity: 1.0,
    backgroundColor: "white"
  },
  cardOverlay: {
    position: "fixed",
    marginTop: "5.0em",
    marginLeft: "auto",
    marginRight: "auto",
    top: 0,
    bottom: 0,
    left: 0,
    right: 0,
    display: "block",
    zIndex: 1002,
    textAlign: "center",
    maxWidth: 500,
    height: 350,
    backgroundColor: "white"
  },
  selectedListOverlay: {
    position: "fixed",
    marginTop: "15.0em",
    marginLeft: "auto",
    marginRight: "auto",
    top: 0,
    bottom: 0,
    left: 0,
    right: 0,
    display: "block",
    zIndex: 1002,
    textAlign: "center",
    maxWidth: 500
  },
  backgroundOverlay: {
    position: "fixed",
    top: 0,
    bottom: 0,
    left: 0,
    right: 0,
    backgroundColor: "#1f1f1f",
    opacity: 0.7,
    display: "block",
    zIndex: 1001,
    textAlign: "center"
  },
  homePaperFirst: {
    marginTop: theme.spacing(10),
    marginLeft: theme.spacing(4),
    marginRight: theme.spacing(4),
    padding: theme.spacing(2)
  },
  homePaper: {
    marginTop: theme.spacing(3),
    marginLeft: theme.spacing(4),
    marginRight: theme.spacing(4),
    padding: theme.spacing(2)
  },
  formPaper: {
    marginTop: theme.spacing(10),
    marginLeft: theme.spacing(1),
    marginRight: theme.spacing(1),
    padding: theme.spacing(1)
  },
  table: {
    whiteSpace: "nowrap"
  },
  tableRow: {
    height: 48
  },
  tableActions: {
    height: "3.0em"
  },
  paginationRight: {
    float: "right"
  },
  tableCellHiddenSm: {
    [theme.breakpoints.down('sm')]: {
      display: "none"
    }
  },
  hiddenCell: {
    display: "none"
  },
  button: {
    marginTop: theme.spacing(1),
    marginRight: theme.spacing(1)
  },
  buttonProgress: {
    color: green[500],
    position: "relative",
    right: 5
  },
  successMessage: {
    color: green[500]
  },
  cardClose: {
    position: "absolute",
    right: 5
  },
  instructionsSmall: {
    fontSize: 10,
    fontWeight: "bold"
  },
  iconBlack: {
    color: "black"
  },
  accountIcon: {
    marginTop: theme.spacing(2),
    marginLeft: theme.spacing(2)
  },
  iconText: {
    display: "inline-block",
    marginLeft: "1em",
    top: -5,
    position: "relative"
  }
});

const iconHeight = 30;

class App extends Component {
  constructor(props) {
    super(props);
    this.state = {
      state: { drawerOpen: false },
      error: false,
      errorMessage: "",
      signedin:false,
      status: "",
      deviceFilter: "",
      maxrows: 5,
      devices: [],
      selectedDevices: [],
      lastId: 0,
      previousLastIds: [],
      previousLastId:0,
      previousActive: false,
      nextActive: false,
      allChecked: false,
      deviceId:"",
      otp: "",
      loading: false,
      administrators: [],
      deviceCount: 0,
      token:""
    }
  }

  handleDrawerToggle = () => {
    this.setState(state => ({ drawerOpen: !state.drawerOpen }));
  };

  valueChanged = (event, field) => {
    this.setState({ [field]: event.target.value });
  }

  getDeviceCount = () => {
    return axios.get(apibase + "/manage/count-devices", {
      headers: {
        'Authorization': 'Bearer ' + this.state.token
      }
    });
  }

  getAdministrators = () => {
    return axios.get(apibase + "/manage/list-administrators", {
      headers: {
        'Authorization': 'Bearer ' + this.state.token
      }
    });
  }

  signin = (event) => {
    var $this = this;
    if (!this.state.deviceId || this.state.deviceId.length === 0) {
      this.setState({
        deviceIdError: true,
        deviceIdErrorText: "Device ID/Alias is required"
      });
      return;
    }

    if (!this.state.otp || this.state.otp.length === 0) {
      this.setState({
        otpError: true,
        otpErrorText: "OTP Code is required"
      });
      return;
    }

    this.setState({
      loading: true
    });

    axios.post(apibase + "/management-login", {
      deviceId: this.state.deviceId,
      otp: this.state.otp
    }).then(function (response) {
      $this.setState({
        signedin: true,
        token: response.data.token,
        otp: "",
        loading: false
      });

      $this.refreshDisplay();
    }).catch(function (response) {
      var status = 0;
      if (response && response.response && response.response.status) {
        status = response.response.status;
      } else if (response && response.status) {
        status = response.status;
      }
      if (status === 403) {
        $this.setState({
          error: true,
          errorMessage: "The OTP code was invalid. Please try again",
          otp: "",
          loading: false
        });
      } else {
        $this.setState({
          error: true,
          errorMessage: "There was an error connecting to the server to login. Contact the system administrator with the following code:" + response.status,
          loading: false
        });
        console.log(response);
      }
    });
  }

  signOut = (event) => {
    this.setState({
      error: false,
      errorMessage: "",
      signedin: false,
      status: "",
      deviceFilter: "",
      maxrows: 5,
      devices: [],
      selectedDevices: [],
      lastId: 0,
      previousLastIds: [],
      previousLastId:0,
      previousActive: false,
      nextActive: false,
      allChecked: false,
      deviceId: "",
      otp: "",
      loading: false,
      administrators: [],
      deviceCount: 0,
      token: "",
      selectedListOpen:false
    });
  }

  refreshDisplay = () => {
    var $this = this;
    axios.all([$this.getDeviceCount(), $this.getAdministrators()])
      .then(axios.spread(function (deviceCount, administrators) {
        $this.setState({
          administrators: administrators.data,
          deviceCount: deviceCount.data.count
        })
      })).catch(function (response) {
        $this.handleResponseError(response);
      });
  }

  filterDevices = (lastId = 0) => {
    var query = "maxrows=" + this.state.maxrows;
    query += "&lastid=" + lastId;
    if (this.state.status
      && this.state.status.length !== 0) {
      query += "&status=" + this.state.status;
    }
    if (this.state.deviceFilter
      && this.state.deviceFilter.length !== 0) {
      query += "&device-filter=" + this.state.deviceFilter;
    }
    this.setState({ loading: true });
    return axios.get(apibase + "/manage/get-devices?" + query, {
      headers: {
        'Authorization': 'Bearer ' + this.state.token
      }
    });
  }

  next = (event) => {
    var $this = this;
    this.setState({ loading: true });
    this.filterDevices(this.state.lastId).then(function (response) {
      if (response.data.length > 0) {
        var previousLastIds = $this.state.previousLastIds;
        var previousLastId = $this.state.previousLastId;
        previousLastIds.push(previousLastId);
        previousLastId = $this.state.lastId;
        
        $this.setState({
          devices: response.data,
          allChecked: false,
          lastId: response.data[response.data.length - 1].itemCount,
          previousActive: true,
          loading: false,
          nextActive: response.data.length === $this.state.maxrows,
          previousLastIds: previousLastIds,
          previousLastId: previousLastId
        });
      } else {
        $this.setState({
          nextActive: false,
          loading: false
        });
      }
    }).catch(function (response) {
      $this.handleResponseError(response);
    });
  }

  previous = (event) => {
    var $this = this;
    if (this.state.previousLastIds.length > 0) {
      var previousLastIds = this.state.previousLastIds;
      var previousLastId = previousLastIds.pop();
      this.setState({ loading: true, previousLastIds: previousLastIds });
      this.filterDevices(previousLastId).then(function (response) {
        if (response.data.length > 0) {
          var lastId = response.data[response.data.length - 1].itemCount;
          $this.setState({
            devices: response.data,
            allChecked: false,
            lastId: lastId,
            nextActive: true,
            loading: false,
            previousActive: previousLastIds.length > 0,
            previousLastId: previousLastId
          });
        } else {
          $this.setState({
            previousActive: false,
            loading: false
          });
        }
      }).catch(function (response) {
        $this.handleResponseError(response);
      });
    } else {
      this.setState({ previousActive: false });
    }
  }

  executeRequest = (endpoint, requests) => {
    return axios.post(apibase + "/manage/" + endpoint, requests, {
      headers: {
        'Authorization': 'Bearer ' + this.state.token
      }
    });
  }

  search = (event) => {
    var $this = this;
    $this.setState({
      loading: true,
      lastId: 0
    });
    this.filterDevices().then(function (response) {
      var lastId = 0;
      if (response.data.length > 0) {
        lastId = response.data[response.data.length - 1].itemCount;
      }
      $this.setState({
        devices: response.data,
        allChecked: false,
        lastId: lastId,
        nextActive: response.data.length === $this.state.maxrows,
        loading: false,
        previousLastId:0,
        previousActive: false
      });
    }).catch(function (response) {
      $this.handleResponseError(response);
    });
  }

  resetHome = (event) => {
    this.resetSearch();
    this.refreshDisplay();
  }

  resetSearch = () => {
    if(this.state.devices.length>0){
      this.setState({
        devices: [],
        allChecked: false,
        lastId: 0,
        nextActive: false,
        previousActive: false,
        previousLastIds: [],
        previousLastId:0,
        selectedDevices: []
      });  
    }
  }

  checkConnection = (deviceId) => {
    var $this = this;
    axios.get(apibase + "/manage/check-connection/" + deviceId, {
      headers: {
        'Authorization': 'Bearer ' + this.state.token
      }
    }).then(function (response) {
      var devices = $this.state.devices;
      devices.forEach(function (device, index) {
        if (device.deviceId === deviceId) {
          device.connected = response.data.connected;
        }
      });
      $this.setState({
        devices: devices
      });
    }).catch(function (response) {
      $this.handleResponseError(response);
    })
  }

  handleResponseError = (response) => {
    var status = 0;
    var message = response && response.message ? response.message : "";
    var data = {};
    if (response && response.status) {
      status = response.status;
      data = response.data;
    }
    else if (response && response.response && response.response.status) {
      status = response.response.status;
      data = response.response.data;
    }
    if ((status === 403 && data.message && data.message === "Authorization failed")
      || message.startsWith("Network Error")) {
      this.setState({
        error: true,
        errorMessage: "Your session has expired. Log in again to continue" + (data.message ? ". Additional information: " + data.message : ""),
        signedin: false,
        loading: false
      });
    } else {
      this.setState({
        error: true,
        errorMessage: data.message ? "Error: " + data.message : ("There was an error connecting to the server. Contact the system administrator with the following code:"
          + status),
        loading: false
      });
      console.log(this);
      console.log(response);
    }
  }

  allCheckboxChanged = () => {
    var currentState = !this.state.allChecked;
    var devices = this.state.devices;
    devices.forEach(function (element, index) {
      element.allChecked = currentState;
      element.checked = element.allChecked;
    });
    this.setState({ allChecked: currentState, devices: devices });
  }

  itemCheck = (id, checked) => {
    this.state.devices.forEach(function (element, index) {
      if (element.deviceId === id) {
        element.checked = checked;
      }
    });
  }

  addSelected = () => {
    var selectedDevices = this.state.selectedDevices;
    this.state.devices.forEach(function(device, index){
      if(device.checked){
        selectedDevices.push(device);
      }
    });
    this.setState({
      selectedDevices:selectedDevices
    });
  }

  clearSelected = () => {
    this.setState({
      selectedDevices:[]
    });
  }

  openSelectedList = () => {
    if(this.state.selectedDevices.length>0){
      this.setState({
        selectedListOpen:true
      });
    }
  }

  closeSelectedList = () => {
    this.setState({
      selectedListOpen:false
    });
  }

  handleSnackbarClose = (event, reason) => {
    if (reason === 'clickaway') {
      return;
    }
    this.setState({ error: false });
  }

  render() {
    const { classes } = this.props;
    return (
      <div>
        {this.state.signedin ?
          <div>
            <AppBar position="fixed" color="default" >
              <Toolbar>
                <IconButton className={classes.menuButton}
                  onClick={this.handleDrawerToggle}
                  color="inherit" aria-label="Menu">
                  <MenuIcon />
                </IconButton>
                <img src={LogoSVG} alt="Quakearts Logo" height={50} />
                TOTP Device Management
              </Toolbar>
            </AppBar>
            <nav>
              <Drawer open={this.state.drawerOpen}
                onClick={this.handleDrawerToggle}
                variant="temporary">
                <div>
                  <div className={classes.toolbar}>
                    <div className={classes.accountIcon}>
                      <AccountCircleIcon color="secondary" />
                      <Typography className={classes.iconText}>{this.state.deviceId}</Typography>
                    </div>
                  </div>
                  <Divider />
                  <List>
                    <ListItem button component={Link} onClick={this.resetHome} to="/">
                      <ListItemIcon><HomeIcon className={classes.iconBlack} /></ListItemIcon>
                      <ListItemText>Home</ListItemText>
                    </ListItem>
                    <ListItem button component={Link} to="/assign">
                      <ListItemIcon><img src={AddAliasSVG} alt="Assign Aliases" height={iconHeight} /></ListItemIcon>
                      <ListItemText>Assign Aliases</ListItemText>
                    </ListItem>
                    <ListItem button component={Link} to="/unassign">
                      <ListItemIcon><img src={RemoveAliasSVG} alt="Un-assign Aliases" height={iconHeight} /></ListItemIcon>
                      <ListItemText>Un-assign Aliases</ListItemText>
                    </ListItem>
                    <ListItem button component={Link} to="/lock">
                      <ListItemIcon><img src={LockDeviceSVG} alt="Lock Devices" height={iconHeight} /></ListItemIcon>
                      <ListItemText>Lock Devices</ListItemText>
                    </ListItem>
                    <ListItem button component={Link} to="/unlock">
                      <ListItemIcon><img src={UnLockDeviceSVG} alt="Un-lock Devices" height={iconHeight} /></ListItemIcon>
                      <ListItemText>Un-lock Devices</ListItemText>
                    </ListItem>
                    <ListItem button component={Link} to="/deactivate">
                      <ListItemIcon><img src={DeactivateDeviceSVG} alt="De-Activate Devices" height={iconHeight} /></ListItemIcon>
                      <ListItemText>De-Activate Devices</ListItemText>
                    </ListItem>
                    <ListItem button component={Link} to="/addadmin">
                      <ListItemIcon><GroupAddIcon className={classes.iconBlack} /></ListItemIcon>
                      <ListItemText>Add Administrator Devices</ListItemText>
                    </ListItem>
                    <ListItem button component={Link} to="/removeadmin">
                      <ListItemIcon><PermIdentityIcon className={classes.iconBlack} /></ListItemIcon>
                      <ListItemText>Remove Administrator Devices</ListItemText>
                    </ListItem>
                    <ListItem button onClick={this.signOut}>
                      <ListItemIcon><ExitToAppIcon className={classes.iconBlack} /></ListItemIcon>
                      <ListItemText>Sign Out</ListItemText>
                    </ListItem>
                  </List>
                </div>
              </Drawer>
            </nav>
            <Route exact path="/" render={() => <HomePage classes={classes} devices={this.state.devices} status={this.state.status}
              deviceFilter={this.state.deviceFilter} search={event => this.search(event)} valueChanged={this.valueChanged}
              administrators={this.state.administrators} deviceCount={this.state.deviceCount} maxrows={this.state.maxrows} 
              previous={this.previous} next={this.next} previousActive={this.state.previousActive} nextActive={this.state.nextActive} 
              allCheckboxChanged={this.allCheckboxChanged} allChecked={this.state.allChecked} itemCheck={this.itemCheck} 
              loading={this.state.loading} checkConnection={this.checkConnection} addSelected={this.addSelected} 
              clearSelected={this.clearSelected} openSelectedList={this.openSelectedList} closeSelectedList={this.closeSelectedList} 
              selectedListOpen={this.state.selectedListOpen} resetSearch={this.resetSearch} selectedDevices={this.state.selectedDevices} />} />
            <Route exact path="/assign" render={() => <DeviceForm classes={classes}
              aliasFieldName="Assigned Alias" showDeviceId={true} showAlias={true} devices={this.state.devices} selectedDevices={this.state.selectedDevices} 
              requestType="Assign Aliases" executeRequest={this.executeRequest} handleResponseError={this.handleResponseError} endpoint="assign-alias" />} />
            <Route exact path="/unassign" render={() => <DeviceForm classes={classes}
              aliasFieldName="Assigned Alias" showDeviceId={false} showAlias={true} devices={this.state.devices} selectedDevices={this.state.selectedDevices} 
              requestType="Un-assign Alias" executeRequest={this.executeRequest} handleResponseError={this.handleResponseError} endpoint="unassign-alias" />} />
            <Route exact path="/lock" render={() => <DeviceForm classes={classes}
              aliasFieldName="" showDeviceId={true} showAlias={false} devices={this.state.devices} selectedDevices={this.state.selectedDevices} 
              requestType="Lock Devices" executeRequest={this.executeRequest} handleResponseError={this.handleResponseError} endpoint="lock" />} />
            <Route exact path="/unlock" render={() => <DeviceForm classes={classes}
              aliasFieldName="" showDeviceId={true} showAlias={false} devices={this.state.devices} selectedDevices={this.state.selectedDevices} 
              requestType="Un-lock Devices" executeRequest={this.executeRequest} handleResponseError={this.handleResponseError} endpoint="unlock" />} />
            <Route exact path="/deactivate" render={() => <DeviceForm classes={classes}
              aliasFieldName="" showDeviceId={true} showAlias={false} devices={this.state.devices} selectedDevices={this.state.selectedDevices} 
              requestType="De-Activate Devices" executeRequest={this.executeRequest} handleResponseError={this.handleResponseError} endpoint="deactivate" />} />
            <Route exact path="/addadmin" render={() => <DeviceForm classes={classes}
              aliasFieldName="Administrator Name" showDeviceId={true} showAlias={true} devices={this.state.devices} selectedDevices={this.state.selectedDevices} 
              requestType="Add Administrator Devices" executeRequest={this.executeRequest} handleResponseError={this.handleResponseError} endpoint="add-as-admin" />} />
            <Route exact path="/removeadmin" render={() => <DeviceForm classes={classes}
              aliasFieldName="" showDeviceId={true} showAlias={false} devices={this.state.devices}
              requestType="Remove Administrator Devices" executeRequest={this.executeRequest}
              handleResponseError={this.handleResponseError} endpoint="remove-as-admin" />} />
          </div>
          :
          <div>
            <Grid container spacing={8}>
              <Grid item sm={3} lg={4} />
              <Grid item xs={12} sm={6} lg={4}>
                <Card className={classes.card}>
                  <CardContent>
                    <div>
                      <img src={LogoSVG} alt="Quakearts Logo" height={50} /><Typography className={classes.iconText}>Sign in to TOTP Device Management</Typography>
                    </div>
                    <TextField label="Device ID/Alias"
                      fullWidth={true}
                      value={this.state.deviceId}
                      className={classes.textField}
                      placeholder="123456789012345"
                      margin="normal"
                      required={true}
                      onChange={event => this.valueChanged(event, "deviceId")}
                      helperText={this.state.deviceIdErrorText}
                      error={this.state.deviceIdError} />
                    <TextField label="OTP Code"
                      fullWidth={true}
                      value={this.state.otp}
                      className={classes.textField}
                      margin="normal"
                      required={true}
                      type="password"
                      onChange={event => this.valueChanged(event, "otp")}
                      helperText={this.state.otpErrorText}
                      error={this.state.otpError} />
                  </CardContent>
                  <CardActions>
                    <Button color="primary" size="small" variant="contained"
                      onClick={event => this.signin(event)} aria-label="Edit">
                      {this.state.loading ?
                        <CircularProgress size={24} className={classes.buttonProgress} /> :
                        <ArrowForwardIosIcon />} Sign In
                    </Button>
                  </CardActions>
                </Card>
              </Grid>
            </Grid>
          </div>}
        <Snackbar
          variant="error"
          anchorOrigin={{
            vertical: 'top',
            horizontal: 'center',
          }}
          open={this.state.error}
          autoHideDuration={6000}
          onClose={this.handleSnackbarClose}
          ContentProps={{
            'aria-describedby': 'message-id',
          }}
          message={<span id="message-id">{this.state.errorMessage}</span>}
          action={[
            <IconButton
              key="close"
              aria-label="Close"
              color="inherit"
              className={classes.close}
              onClick={this.handleSnackbarClose}>
              <CloseIcon />
            </IconButton>,
          ]} />
      </div>
    );
  }
}
App.propTypes = {
  classes: PropTypes.object.isRequired,
};

export default withRouter(withStyles(styles)(App));
