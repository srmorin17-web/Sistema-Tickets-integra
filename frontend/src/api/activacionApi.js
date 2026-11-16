import axiosClient from './axiosClient';

export const activarCuenta = ({ token, contrasenaNueva }) =>
  axiosClient.post('/auth/activar-cuenta', { token, contrasenaNueva });

